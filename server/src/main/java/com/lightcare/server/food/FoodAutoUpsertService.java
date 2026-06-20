package com.lightcare.server.food;

import com.lightcare.server.recognize.RecognizeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 拍照识别后自动入库（PR-D）。
 *
 * 挂点：MealRecognitionExecutor.enqueue 在 writeBackInNewTx 成功后调用本服务。
 * 策略：把每条 RecognizedItem 当"1 份"，displayName 带克数后缀（"米饭(180g)"）。
 *   - 无同名候选 → 直接 insert (source=AI, conflictStatus=RESOLVED)
 *   - 同名 + 四项营养全等 → 跳过（不覆盖、不报冲突）
 *   - 同名 + 营养不同 → insert 一条 conflictStatus=PENDING_CONFLICT（不动已有项，等用户决定换名/覆盖/跳过）
 *
 * 重名判定复用 FoodController.nutritionEquals（同包 package-private static）保持口径一致。
 * 全程独立 try（调用方包），失败不影响 meal 回写。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FoodAutoUpsertService {

    private final FoodRepository foodRepo;

    /**
     * 把识别结果尝试入食物库。返回入库 / 跳过 / 冲突计数，仅用于日志。
     */
    @Transactional
    public void autoUpsert(long userId, List<RecognizeService.RecognizedItem> recognized) {
        if (recognized == null || recognized.isEmpty()) return;
        int inserted = 0, skipped = 0, conflicted = 0;
        for (RecognizeService.RecognizedItem r : recognized) {
            if (r.weightG() <= 0 || r.kcal() <= 0) continue;   // 无效项不入库
            String name = r.name() + "(" + r.weightG() + "g)";
            List<FoodEntity> candidates = foodRepo.findByNameIgnoreCase(userId, name);
            boolean nutritionMatch = candidates.stream().anyMatch(c ->
                FoodController.nutritionEquals(c.getKcal(), r.kcal())
                && FoodController.nutritionEquals(c.getProteinG(), r.proteinG())
                && FoodController.nutritionEquals(c.getFatG(), r.fatG())
                && FoodController.nutritionEquals(c.getCarbG(), r.carbG()));
            if (nutritionMatch) {
                skipped++;   // 营养全等 → 跳过
                continue;
            }
            FoodEntity f = new FoodEntity();
            f.setOwnerUserId(userId);
            f.setKey("ai_" + System.currentTimeMillis() + "_" + Math.abs(name.hashCode()));
            f.setDisplayName(name);
            f.setCategory(r.category() == null || r.category().isBlank() ? "其他" : r.category());
            f.setSource(FoodEntity.Source.AI);
            f.setPerServingG(r.weightG());
            f.setKcal(r.kcal());
            f.setProteinG(r.proteinG());
            f.setFatG(r.fatG());
            f.setCarbG(r.carbG());
            f.setFiberG(r.fiberG());
            f.setWaterMl(r.waterMl());
            f.setVegServings("蔬果".equals(r.category()) ? r.weightG() / 80 : 0);
            // 有同名候选但营养不同 → 标冲突；无任何候选 → 正常入库
            f.setConflictStatus(candidates.isEmpty()
                ? FoodEntity.ConflictStatus.RESOLVED
                : FoodEntity.ConflictStatus.PENDING_CONFLICT);
            foodRepo.save(f);
            if (candidates.isEmpty()) inserted++; else conflicted++;
        }
        log.info("[FOOD-AUTO] userId={} recognized={} inserted={} skipped={} conflicted={}",
            userId, recognized.size(), inserted, skipped, conflicted);
    }
}
