package com.lightcare.server.meal;

import com.lightcare.server.food.FoodAutoUpsertService;
import com.lightcare.server.profile.ProfileEntity;
import com.lightcare.server.profile.ProfileRepository;
import com.lightcare.server.recognize.RecognizeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * PR3: 拍照异步识别。
 *
 * 流程：MealController.createFromPhoto 收到 multipart 后：
 *   1) 同步保存 meal（status=PENDING, 营养 0）
 *   2) 持久化图片到 uploadDir
 *   3) 调 enqueue(mealId, imageBytes, previewItems) → 入队
 *
 * 后台线程 (meal-rec-* pool)：
 *   4) RecognizeService.recognizeBytes → List<RecognizedItem>
 *   5) 按 name+category 跟 previewItems 匹配，套用 M3 营养
 *   6) 写回 meal（kcal/protein/fat/carb/fiber/waterMl/vegServings + status=DONE）
 *   7) PR-D: FoodAutoUpsertService.autoUpsert → 识别出的食物入 lc_food_item
 *      （同名同营养跳过，同名不同营养标 PENDING_CONFLICT 等用户决定）
 *
 * 失败：写 status=FAILED，保留 summary 与 previewItems 估算的营养字段不动。
 *
 * 事务边界：enqueue 是 @Async，writeBackInNewTx 是 @Transactional(REQUIRES_NEW)。
 * Spring 自调用不走代理 → 必须经 applicationContext.getBean(Self) 拿代理对象调。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MealRecognitionExecutor {

    private final RecognizeService recognizeService;
    private final MealRepository mealRepo;
    private final ApplicationContext appContext;
    private final ProfileRepository profileRepo;       // PR-D: 反查 meal.profileId → ownerUserId
    private final FoodAutoUpsertService foodAutoUpsert;  // PR-D: 识别完自动入食物库

    @Value("${lightcare.upload-dir:uploads}")
    private String uploadDir;

    /** 用户上传的预览项（PR3: 拍完/相册选完，app 端编辑后提交）。 */
    public record PreviewItem(String name, String category, int weightG) {}

    /** M3 识别后聚合的营养。 */
    private record NutritionAgg(
        int kcal, double protein, double fat, double carb, double fiber, int waterMl, int vegServings
    ) {}

    @Async("mealRecExecutorPool")
    public void enqueue(long mealId, byte[] imageBytes, List<PreviewItem> previewItems) {
        log.info("[PR3] 异步识别开始 mealId={}, items={}", mealId, previewItems == null ? 0 : previewItems.size());
        try {
            List<RecognizeService.RecognizedItem> recognized =
                recognizeService.recognizeBytes(imageBytes, "image/jpeg");
            NutritionAgg agg = aggregate(previewItems, recognized);
            appContext.getBean(MealRecognitionExecutor.class).writeBackInNewTx(mealId, agg);
            log.info("[PR3] 异步识别完成 mealId={}, kcal={}, water={}ml", mealId, agg.kcal(), agg.waterMl());

            // PR-D: 识别完自动入食物库（独立 try，失败不影响 meal 回写）。
            try {
                Long userId = resolveOwnerUserId(mealId);
                if (userId != null) {
                    foodAutoUpsert.autoUpsert(userId, recognized);
                }
            } catch (Throwable fe) {
                log.warn("[FOOD-AUTO] 自动入库失败 mealId={}, err={}", mealId, fe.getMessage());
            }
        } catch (Throwable e) {
            log.warn("[PR3] 异步识别失败 mealId={}, err={}", mealId, e.getMessage());
            try {
                appContext.getBean(MealRecognitionExecutor.class).markFailedInNewTx(mealId);
                log.info("[PR3] 标记 FAILED 完成 mealId={}", mealId);
            } catch (Throwable ex) {
                log.error("[PR3] 标记 FAILED 也失败 mealId={}", mealId, ex);
            }
        }
    }

    /** mealId → meal.profileId → profile.ownerUserId（食物库归属用户）。 */
    private Long resolveOwnerUserId(long mealId) {
        MealEntity m = mealRepo.findById(mealId).orElse(null);
        if (m == null || m.getProfileId() == null) return null;
        ProfileEntity p = profileRepo.findById(m.getProfileId()).orElse(null);
        return p == null ? null : p.getOwnerUserId();
    }

    /**
     * 聚合策略：
     *   - 用户没填 preview（空 list）→ 直接用 M3 识别结果（按 M3 weightG 原值汇总）
     *   - 有 preview：按 (name 归一化 + category) 匹配 M3 项
     *     - 匹配上：用 M3 的 weightG 作分母换算到用户的 weightG（保持 M3 营养密度）
     *     - 没匹配上：按 category 默认密度粗估（kcal ≈ w*1.5, protein ≈ w*0.05, fat ≈ w*0.05, carb ≈ w*0.2, water 按 category）
     *   - 蔬果 veg_servings：weightG/80 累加（preview/matched 两种来源都算）
     */
    private NutritionAgg aggregate(List<PreviewItem> previews, List<RecognizeService.RecognizedItem> recognized) {
        if (previews == null || previews.isEmpty()) {
            // 空 preview：完全用 M3 结果
            if (recognized == null || recognized.isEmpty()) {
                return new NutritionAgg(0, 0, 0, 0, 0, 0, 0);
            }
            int kcal = 0; double p = 0, f = 0, c = 0, fib = 0; int water = 0; int veg = 0;
            for (var r : recognized) {
                kcal += r.kcal();
                p += round1(r.proteinG());
                f += round1(r.fatG());
                c += round1(r.carbG());
                fib += round1(r.fiberG());
                water += r.waterMl();
                if ("蔬果".equals(r.category())) veg += r.weightG() / 80;
            }
            return new NutritionAgg(kcal, p, f, c, fib, water, veg);
        }
        int kcal = 0;
        double p = 0, f = 0, c = 0, fib = 0;
        int water = 0;
        int veg = 0;
        for (PreviewItem pi : previews) {
            String key = norm(pi.name());
            RecognizeService.RecognizedItem hit = recognized.stream()
                .filter(r -> norm(r.name()).equals(key) && sameCat(r.category(), pi.category()))
                .findFirst()
                .orElse(null);
            if (hit != null && hit.weightG() > 0) {
                // 用 M3 营养密度 × 用户 weightG 换算
                double mult = pi.weightG() / (double) hit.weightG();
                kcal += (int) Math.round(hit.kcal() * mult);
                p += round1(hit.proteinG() * mult);
                f += round1(hit.fatG() * mult);
                c += round1(hit.carbG() * mult);
                fib += round1(hit.fiberG() * mult);
                water += (int) Math.round(hit.waterMl() * mult);
            } else {
                // 没匹配上：按 category 默认密度
                double k = pi.weightG() * 1.5;
                double pp = pi.weightG() * 0.05;
                double ff = pi.weightG() * 0.05;
                double cc = pi.weightG() * 0.2;
                int ww = estimateWater(pi.category(), pi.weightG());
                kcal += (int) Math.round(k);
                p += round1(pp);
                f += round1(ff);
                c += round1(cc);
                water += ww;
            }
            if ("蔬果".equals(pi.category())) {
                veg += pi.weightG() / 80;
            }
        }
        return new NutritionAgg(kcal, p, f, c, fib, water, veg);
    }

    private static int estimateWater(String category, int weightG) {
        if (category == null) return (int) (weightG * 0.5);
        return switch (category) {
            case "饮品" -> (int) (weightG * 0.95);   // 汤/粥/饮料自由水
            case "蔬果" -> (int) (weightG * 0.85);
            case "主食" -> (int) (weightG * 0.5);    // 米饭 50%
            case "蛋白" -> (int) (weightG * 0.65);
            case "坚果" -> (int) (weightG * 0.05);
            default -> (int) (weightG * 0.5);
        };
    }

    private static String norm(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase(Locale.ROOT)
            .replaceAll("[\\s+＋+]+", "")
            .replaceAll("[（）()\\[\\]【】]", "");
    }

    private static boolean sameCat(String a, String b) {
        if (a == null || b == null) return false;
        return a.trim().equals(b.trim());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeBackInNewTx(long mealId, NutritionAgg agg) {
        MealEntity m = mealRepo.findById(mealId).orElse(null);
        if (m == null) return;
        // 零值守卫：识别返回空（preview 空 + M3 空）时 agg 全 0，不要用 0 覆盖 createFromPhoto
        // 已经预填的粗估营养。标 FAILED 让前端提示「识别失败」，保留原粗估值。
        if (agg.kcal() == 0 && agg.protein() == 0 && agg.fat() == 0 && agg.carb() == 0) {
            log.warn("[PR3] 识别结果全 0，保留原粗估值，标 FAILED mealId={}", mealId);
            m.setRecognitionStatus(MealEntity.RecognitionStatus.FAILED);
            mealRepo.save(m);
            return;
        }
        m.setKcal(agg.kcal());
        m.setProteinG(agg.protein());
        m.setFatG(agg.fat());
        m.setCarbG(agg.carb());
        m.setFiberG(agg.fiber());
        m.setWaterMl(agg.waterMl());
        m.setVegServings(agg.vegServings());
        m.setRecognitionStatus(MealEntity.RecognitionStatus.DONE);
        mealRepo.save(m);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailedInNewTx(long mealId) {
        MealEntity m = mealRepo.findById(mealId).orElse(null);
        if (m == null) return;
        m.setRecognitionStatus(MealEntity.RecognitionStatus.FAILED);
        mealRepo.save(m);
    }

    private static double round1(double v) { return Math.round(v * 10.0) / 10.0; }
}
