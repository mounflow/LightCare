package com.lightcare.server.food;

import com.lightcare.server.common.ApiError;
import com.lightcare.server.common.ApiException;
import com.lightcare.server.common.ApiResponse;
import com.lightcare.server.common.CurrentUserAnnotation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 食物库 CRUD（PR-C）。
 *
 * 作用域：user 级 + 全局种子（owner_user_id IS NULL）。
 * 鉴权：沿用 @CurrentUserAnnotation 拿 userId（裸 header，无 JWT），当 owner_user_id。
 * 种子（is_default=TRUE）只读，不可改不可删。
 *
 * 重名判定：display_name 相同 + 四项营养（kcal/protein/fat/carb）±5% 全等才算重复。
 *   - create 命中重名 → 直接返回 code=40901 + data=现有项（不抛异常，避免 GlobalExceptionHandler 丢 data）
 *   - 拍照自动入库（PR-D 的 FoodAutoUpsertService）复用同名判定逻辑：全等跳过，否则标 PENDING_CONFLICT
 */
@RestController
@RequestMapping("/v1/foods")
@RequiredArgsConstructor
@Slf4j
public class FoodController {

    private final FoodRepository foodRepo;

    /** 容差：四项营养相对误差 ≤ 5% 视为相等。 */
    private static final double TOLERANCE = 0.05;
    /** 冲突错误码（client 判 code==40901 走"换名/覆盖"流程）。 */
    private static final int CODE_DUPLICATE = 40901;

    public record FoodDto(
        long id, Long ownerUserId, String key, String displayName, String category,
        String source, int perServingG, int kcal,
        double proteinG, double fatG, double carbG, double fiberG,
        int waterMl, int vegServings,
        boolean isDefault, String conflictStatus
    ) {}

    public record UpsertFoodReq(
        String key, String displayName, String category,
        Integer perServingG, Integer kcal,
        Double proteinG, Double fatG, Double carbG, Double fiberG,
        Integer waterMl, Integer vegServings
    ) {}

    /** 列表：自己的 + 全局种子。 */
    @GetMapping
    public ApiResponse<List<FoodDto>> list(@CurrentUserAnnotation long userId) {
        List<FoodDto> dtos = foodRepo.findVisible(userId).stream().map(this::toDto).toList();
        return ApiResponse.ok(dtos);
    }

    /** 搜索：displayName / category 包含 q。 */
    @GetMapping("/search")
    public ApiResponse<List<FoodDto>> search(@CurrentUserAnnotation long userId,
                                             @RequestParam(required = false, defaultValue = "") String q,
                                             @RequestParam(defaultValue = "20") int limit) {
        String k = q == null ? "" : q.trim();
        List<FoodDto> all = foodRepo.findVisible(userId).stream().map(this::toDto).toList();
        List<FoodDto> filtered = k.isEmpty()
            ? all
            : all.stream().filter(f -> f.displayName().contains(k) || f.category().contains(k)).toList();
        return ApiResponse.ok(filtered.stream().limit(Math.max(1, limit)).toList());
    }

    /**
     * 新增（手加）。命中重名（四项营养全等）→ 返回 code=40901 + data=现有项，client 决定换名/覆盖。
     * 同名但营养不同 → 不算重名，允许新建（用户可能想记同名的另一个版本）。
     */
    @PostMapping
    @Transactional
    public ApiResponse<FoodDto> create(@CurrentUserAnnotation long userId, @RequestBody UpsertFoodReq req) {
        if (req.displayName() == null || req.displayName().isBlank()) {
            throw new ApiException(ApiError.BAD_REQUEST, "请填写食物名称");
        }
        // 重名校验：同名 + 四项营养全等 → 拒绝（返回现有项让 client 决定）
        FoodEntity dup = findDuplicate(userId, req.displayName().trim(),
            n(req.kcal()), n(req.proteinG()), n(req.fatG()), n(req.carbG()));
        if (dup != null) {
            return new ApiResponse<>(CODE_DUPLICATE, "已有同名且营养相同的食物", toDto(dup));
        }
        FoodEntity f = new FoodEntity();
        f.setOwnerUserId(userId);
        f.setKey(req.key() != null && !req.key().isBlank() ? req.key() : "custom_" + System.currentTimeMillis());
        f.setSource(FoodEntity.Source.MANUAL);
        applyReq(f, req);
        foodRepo.save(f);
        return ApiResponse.ok(toDto(f));
    }

    /** 更新（只能改自己的，种子不可改）。 */
    @PutMapping("/{id}")
    @Transactional
    public ApiResponse<FoodDto> update(@CurrentUserAnnotation long userId,
                                       @PathVariable long id,
                                       @RequestBody UpsertFoodReq req) {
        FoodEntity f = mustOwn(userId, id);
        if (req.displayName() != null && !req.displayName().isBlank()) {
            f.setDisplayName(req.displayName().trim());
        }
        applyReq(f, req);
        foodRepo.save(f);
        return ApiResponse.ok(toDto(f));
    }

    /** 删除（只能删自己的，种子不可删）。 */
    @DeleteMapping("/{id}")
    @Transactional
    public ApiResponse<Void> delete(@CurrentUserAnnotation long userId, @PathVariable long id) {
        FoodEntity f = mustOwn(userId, id);
        foodRepo.delete(f);
        return ApiResponse.ok(null);
    }

    /**
     * 待处理冲突列表（拍照自动入库命中"同名不同营养"时，client 轮询这个）。
     */
    @GetMapping("/conflicts")
    public ApiResponse<List<FoodDto>> conflicts(@CurrentUserAnnotation long userId) {
        List<FoodDto> dtos = foodRepo
            .findByOwnerUserIdAndConflictStatus(userId, FoodEntity.ConflictStatus.PENDING_CONFLICT)
            .stream().map(this::toDto).toList();
        return ApiResponse.ok(dtos);
    }

    /**
     * 解决冲突：rename（改个名后 RESOLVED）/ overwrite（用冲突项覆盖同名现有项）/ skip（删掉冲突项不入库）。
     * action: RENAME | OVERWRITE | SKIP；RENAME 时 newName 必填。
     */
    @PostMapping("/{id}/resolve")
    @Transactional
    public ApiResponse<Void> resolve(@CurrentUserAnnotation long userId,
                                     @PathVariable long id,
                                     @RequestParam String action,
                                     @RequestParam(required = false) String newName) {
        FoodEntity conflict = mustOwn(userId, id);
        if (conflict.getConflictStatus() != FoodEntity.ConflictStatus.PENDING_CONFLICT) {
            throw new ApiException(ApiError.BAD_REQUEST, "该食物不是待处理冲突");
        }
        String act = action == null ? "" : action.trim().toUpperCase();
        switch (act) {
            case "RENAME" -> {
                if (newName == null || newName.isBlank()) {
                    throw new ApiException(ApiError.BAD_REQUEST, "换名需提供 newName");
                }
                conflict.setDisplayName(newName.trim());
                conflict.setConflictStatus(FoodEntity.ConflictStatus.RESOLVED);
                foodRepo.save(conflict);
            }
            case "OVERWRITE" -> {
                // 用冲突项的营养覆盖同名现有项（保留现有项的 id），然后删掉冲突副本
                FoodEntity existing = foodRepo.findByNameIgnoreCase(userId, originalNameOf(conflict))
                    .stream().filter(e -> !e.getId().equals(conflict.getId())
                        && e.getConflictStatus() == FoodEntity.ConflictStatus.RESOLVED)
                    .findFirst().orElse(null);
                if (existing != null) {
                    copyNutrition(existing, conflict);
                    foodRepo.save(existing);
                }
                foodRepo.delete(conflict);
            }
            case "SKIP" -> foodRepo.delete(conflict);
            default -> throw new ApiException(ApiError.BAD_REQUEST, "action 必须是 RENAME/OVERWRITE/SKIP");
        }
        return ApiResponse.ok(null);
    }

    // ===== 共享：重名判定（PR-D 的 FoodAutoUpsertService 也会复用同口径） =====

    /**
     * 在"自己的 + 全局种子"中找同名候选，再按四项营养 ±5% 判定是否营养全等。
     * @return 营养全等的现有项；没有则 null。
     */
    FoodEntity findDuplicate(long userId, String displayName,
                             double kcal, double proteinG, double fatG, double carbG) {
        List<FoodEntity> candidates = foodRepo.findByNameIgnoreCase(userId, displayName);
        for (FoodEntity c : candidates) {
            if (nutritionEquals(c.getKcal(), kcal)
                && nutritionEquals(c.getProteinG(), proteinG)
                && nutritionEquals(c.getFatG(), fatG)
                && nutritionEquals(c.getCarbG(), carbG)) {
                return c;
            }
        }
        return null;
    }

    /**
     * 营养相等判定：相对误差 ≤ TOLERANCE。0 值特殊处理（都为 0 才等，避免 0 vs 5g 在 ±5% 下误判为相等）。
     */
    static boolean nutritionEquals(double a, double b) {
        if (a == 0.0 || b == 0.0) return a == 0.0 && b == 0.0;
        return Math.abs(a - b) / Math.max(a, b) <= TOLERANCE;
    }

    // ===== helpers =====

    private FoodEntity mustOwn(long userId, long id) {
        FoodEntity f = foodRepo.findById(id)
            .orElseThrow(() -> new ApiException(ApiError.NOT_FOUND));
        if (f.getOwnerUserId() == null || !f.getOwnerUserId().equals(userId)) {
            // 全局种子或他人食物不可改/删
            throw new ApiException(ApiError.FORBIDDEN);
        }
        return f;
    }

    private void applyReq(FoodEntity f, UpsertFoodReq req) {
        if (req.displayName() != null && !req.displayName().isBlank()) {
            f.setDisplayName(req.displayName().trim());
        }
        if (req.category() != null && !req.category().isBlank()) f.setCategory(req.category().trim());
        if (req.perServingG() != null) f.setPerServingG(req.perServingG());
        if (req.kcal() != null) f.setKcal(req.kcal());
        if (req.proteinG() != null) f.setProteinG(req.proteinG());
        if (req.fatG() != null) f.setFatG(req.fatG());
        if (req.carbG() != null) f.setCarbG(req.carbG());
        if (req.fiberG() != null) f.setFiberG(req.fiberG());
        if (req.waterMl() != null) f.setWaterMl(req.waterMl());
        if (req.vegServings() != null) f.setVegServings(req.vegServings());
    }

    /** OVERWRITE 时：冲突项的 displayName 是 "原名(克数g)" 形式，取括号前作为现有项名。 */
    private String originalNameOf(FoodEntity conflict) {
        String n = conflict.getDisplayName();
        int idx = n.lastIndexOf('(');
        return idx > 0 ? n.substring(0, idx).trim() : n;
    }

    private void copyNutrition(FoodEntity target, FoodEntity src) {
        target.setKcal(src.getKcal());
        target.setProteinG(src.getProteinG());
        target.setFatG(src.getFatG());
        target.setCarbG(src.getCarbG());
        target.setFiberG(src.getFiberG());
        target.setWaterMl(src.getWaterMl());
        target.setVegServings(src.getVegServings());
        target.setPerServingG(src.getPerServingG());
    }

    private static double n(Number x) { return x == null ? 0.0 : x.doubleValue(); }

    private FoodDto toDto(FoodEntity f) {
        return new FoodDto(
            f.getId(), f.getOwnerUserId(), f.getKey(), f.getDisplayName(), f.getCategory(),
            f.getSource() == null ? "MANUAL" : f.getSource().name(),
            f.getPerServingG(), f.getKcal(),
            f.getProteinG(), f.getFatG(), f.getCarbG(), f.getFiberG(),
            f.getWaterMl(), f.getVegServings(),
            f.isDefault(),
            f.getConflictStatus() == null ? "RESOLVED" : f.getConflictStatus().name()
        );
    }
}
