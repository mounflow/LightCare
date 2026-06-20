package com.lightcare.server.profile;

import com.lightcare.server.common.ApiError;
import com.lightcare.server.common.ApiException;
import com.lightcare.server.common.ApiResponse;
import com.lightcare.server.common.CurrentUserAnnotation;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/v1/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileRepository profileRepo;
    private final UserRepository userRepo;

    public record ProfileDto(
        long id, long ownerUserId, Long managedByUserId,
        String displayName, String avatarUrl, String relation,
        LocalDate birthDate, String gender,
        Integer heightCm, Double weightKg, String activityLevel,
        int proteinTargetG, int vegTargetServings,
        int waterTargetMl, int stepTarget, int calorieTargetKcal
    ) {}

    public record CreateProfileReq(
        String displayName, String avatarUrl, String relation,
        LocalDate birthDate, String gender,
        Integer heightCm, Double weightKg, String activityLevel
    ) {}

    /** 本地化建档：不依赖登录。建一个占位 user + 首份 SELF 档案，返回 userId/profileId 供 App 存本地。 */
    public record BootstrapReq(
        String displayName, LocalDate birthDate, String gender,
        Integer heightCm, Double weightKg, String activityLevel
    ) {}

    public record BootstrapRes(long userId, ProfileDto profile) {}

    public record UpdateGoalsReq(
        Integer proteinTargetG, Integer vegTargetServings,
        Integer waterTargetMl, Integer stepTarget, Integer calorieTargetKcal
    ) {}

    /** 方案 3：单独更新身体数据（身高/体重/年龄/性别/活动量），触发智能目标重算 */
    public record UpdatePhysiqueReq(
        LocalDate birthDate, String gender,
        Integer heightCm, Double weightKg, String activityLevel
    ) {}

    @GetMapping
    public ApiResponse<List<ProfileDto>> list(@CurrentUserAnnotation long userId) {
        var list = profileRepo.findByOwnerUserId(userId).stream()
            .map(this::toDto).toList();
        return ApiResponse.ok(list);
    }

    @PostMapping
    @Transactional
    public ApiResponse<ProfileDto> create(@CurrentUserAnnotation long userId, @RequestBody CreateProfileReq req) {
        if (profileRepo.countByOwnerUserId(userId) >= 4) {
            throw new ApiException(ApiError.BAD_REQUEST, "最多 4 份档案");
        }
        if (req.displayName() == null || req.displayName().isBlank()) {
            throw new ApiException(ApiError.BAD_REQUEST, "请填写档案名");
        }
        ProfileEntity p = new ProfileEntity();
        p.setOwnerUserId(userId);
        p.setDisplayName(req.displayName().trim());
        p.setAvatarUrl(req.avatarUrl());
        p.setRelation(parseRelation(req.relation()));
        p.setBirthDate(req.birthDate());
        p.setGender(req.gender());
        p.setHeightCm(req.heightCm());
        p.setWeightKg(req.weightKg());
        p.setActivityLevel(parseActivity(req.activityLevel()));
        applyPhysiqueBasedTargets(p);
        profileRepo.save(p);
        return ApiResponse.ok(toDto(p));
    }

    /**
     * 本地化建档入口（无鉴权）：本地运行无需登录，App 首次进入时调用。
     * 建一个占位 lc_user（phone/wechat 为 null）+ 一份 SELF 档案，返回 userId + profile。
     * App 拿到后把 ownerUserId 当 userId 存 AuthStore，后续请求带 X-LightCare-User-Id 头即可。
     */
    @PostMapping("/bootstrap")
    @Transactional
    public ApiResponse<BootstrapRes> bootstrap(@RequestBody BootstrapReq req) {
        String name = (req.displayName() == null || req.displayName().isBlank())
            ? "我" : req.displayName().trim();

        UserEntity u = new UserEntity();
        u.setNickname(name);
        u = userRepo.save(u);

        ProfileEntity p = new ProfileEntity();
        p.setOwnerUserId(u.getId());
        p.setDisplayName(name);
        p.setRelation(ProfileEntity.Relation.SELF);
        p.setBirthDate(req.birthDate());
        p.setGender(req.gender());
        p.setHeightCm(req.heightCm());
        p.setWeightKg(req.weightKg());
        p.setActivityLevel(parseActivity(req.activityLevel()));
        applyPhysiqueBasedTargets(p);
        profileRepo.save(p);

        return ApiResponse.ok(new BootstrapRes(u.getId(), toDto(p)));
    }

    /** 更新身体数据（身高/体重/年龄/性别/活动量），触发智能目标重算 */
    @PatchMapping("/{id}/physique")
    @Transactional
    public ApiResponse<ProfileDto> updatePhysique(@CurrentUserAnnotation long userId,
                                                  @PathVariable long id,
                                                  @RequestBody UpdatePhysiqueReq req) {
        ProfileEntity p = mustOwn(userId, id);
        if (req.birthDate() != null) p.setBirthDate(req.birthDate());
        if (req.gender() != null && !req.gender().isBlank()) p.setGender(req.gender());
        if (req.heightCm() != null) p.setHeightCm(req.heightCm());
        if (req.weightKg() != null) p.setWeightKg(req.weightKg());
        if (req.activityLevel() != null) p.setActivityLevel(parseActivity(req.activityLevel()));
        applyPhysiqueBasedTargets(p);   // 用最新 physique 重算所有目标值
        profileRepo.save(p);
        return ApiResponse.ok(toDto(p));
    }

    @PatchMapping("/{id}/goals")
    @Transactional
    public ApiResponse<ProfileDto> updateGoals(@CurrentUserAnnotation long userId,
                                                @PathVariable long id,
                                                @RequestBody UpdateGoalsReq req) {
        ProfileEntity p = mustOwn(userId, id);
        if (req.proteinTargetG()  != null) p.setProteinTargetG(req.proteinTargetG());
        if (req.vegTargetServings() != null) p.setVegTargetServings(req.vegTargetServings());
        if (req.waterTargetMl()   != null) p.setWaterTargetMl(req.waterTargetMl());
        if (req.stepTarget()      != null) p.setStepTarget(req.stepTarget());
        if (req.calorieTargetKcal() != null) p.setCalorieTargetKcal(req.calorieTargetKcal());
        profileRepo.save(p);
        return ApiResponse.ok(toDto(p));
    }

    private ProfileEntity mustOwn(long userId, long profileId) {
        ProfileEntity p = profileRepo.findById(profileId)
            .orElseThrow(() -> new ApiException(ApiError.NOT_FOUND));
        if (p.getOwnerUserId() != userId && !java.util.Objects.equals(p.getManagedByUserId(), userId)) {
            throw new ApiException(ApiError.FORBIDDEN);
        }
        return p;
    }

    private ProfileEntity.Relation parseRelation(String s) {
        if (s == null) return ProfileEntity.Relation.SELF;
        try { return ProfileEntity.Relation.valueOf(s.trim().toUpperCase()); }
        catch (Exception e) { return ProfileEntity.Relation.SELF; }
    }

    private ProfileEntity.ActivityLevel parseActivity(String s) {
        if (s == null || s.isBlank()) return ProfileEntity.ActivityLevel.LIGHT;
        try { return ProfileEntity.ActivityLevel.valueOf(s.trim().toUpperCase()); }
        catch (Exception e) { return ProfileEntity.ActivityLevel.LIGHT; }
    }

    /** PRD §1.4 + 方案 3：Mifflin-St Jeor 公式算 BMR，×活动系数得 TDEE，再分配目标值。
     *  - 身高/体重任一缺失 → 退化为 PRD §1.4 三档默认
     *  - 性别未填 (U) → 按体重 60kg 兜底
     *  - 蛋白 1.6 g/kg；水 35 ml/kg；蔬果按年龄段；步数按活动量
     *  - 热量不直接采 TDEE，减脂缺口 10%，保 1500 大卡下限（防推荐极低） */
    private void applyPhysiqueBasedTargets(ProfileEntity p) {
        Integer h = p.getHeightCm();
        Double w = p.getWeightKg();
        if (h == null || w == null || h <= 0 || w <= 0) { fallbackAgeDefaults(p); return; }

        int age = (p.getBirthDate() != null)
            ? LocalDate.now().getYear() - p.getBirthDate().getYear()
            : 30;   // 兜底
        age = Math.max(age, 5);

        double bmr;
        String g = p.getGender() == null ? "U" : p.getGender().toUpperCase();
        switch (g) {
            case "M" -> bmr = 10 * w + 6.25 * h - 5 * age + 5;
            case "F" -> bmr = 10 * w + 6.25 * h - 5 * age - 161;
            default  -> bmr = 10 * w + 6.25 * h - 5 * age - 78;   // U 兜底取平均
        }

        double tdee = bmr * p.getActivityLevel().factor;
        int targetKcal = (int) Math.max(1500, tdee * 0.90);   // 10% 减脂缺口
        int proteinG  = (int) Math.round(w * 1.6);
        int waterMl   = (int) Math.round(w * 35);
        int steps     = switch (p.getActivityLevel()) {
            case SEDENTARY -> 6000;
            case LIGHT      -> 8000;
            case MODERATE   -> 10000;
            case ACTIVE     -> 12000;
            case VERY_ACTIVE -> 14000;
        };
        int vegServings = (age <= 18) ? 5 : (age <= 50 ? 5 : 4);

        p.setCalorieTargetKcal(targetKcal);
        p.setProteinTargetG(proteinG);
        p.setWaterTargetMl(waterMl);
        p.setStepTarget(steps);
        p.setVegTargetServings(vegServings);
    }

    /** PRD §1.4 退化版：身高/体重未知时的年龄段默认 */
    private void fallbackAgeDefaults(ProfileEntity p) {
        if (p.getBirthDate() == null) return;
        int age = LocalDate.now().getYear() - p.getBirthDate().getYear();
        if (age <= 18) {
            p.setProteinTargetG(60); p.setWaterTargetMl(1500); p.setStepTarget(8000); p.setCalorieTargetKcal(2000);
        } else if (age <= 50) {
            p.setProteinTargetG(60); p.setWaterTargetMl(1700); p.setStepTarget(8000); p.setCalorieTargetKcal(2000);
        } else {
            p.setProteinTargetG(55); p.setWaterTargetMl(1500); p.setStepTarget(6000); p.setCalorieTargetKcal(1800);
        }
    }

    private ProfileDto toDto(ProfileEntity p) {
        return new ProfileDto(
            p.getId(), p.getOwnerUserId(), p.getManagedByUserId(),
            p.getDisplayName(), p.getAvatarUrl(), p.getRelation().name(),
            p.getBirthDate(), p.getGender(),
            p.getHeightCm(), p.getWeightKg(),
            p.getActivityLevel() == null ? null : p.getActivityLevel().name(),
            p.getProteinTargetG(), p.getVegTargetServings(),
            p.getWaterTargetMl(), p.getStepTarget(), p.getCalorieTargetKcal()
        );
    }
}
