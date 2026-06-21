package com.lightcare.server.auth;

import com.lightcare.server.auth.dto.AuthRes;
import com.lightcare.server.auth.dto.AuthMeRes;
import com.lightcare.server.auth.dto.LoginReq;
import com.lightcare.server.auth.dto.RegisterReq;
import com.lightcare.server.common.ApiError;
import com.lightcare.server.common.ApiException;
import com.lightcare.server.profile.ProfileEntity;
import com.lightcare.server.profile.ProfileRepository;
import com.lightcare.server.profile.ProfileTargetCalculator;
import com.lightcare.server.profile.UserEntity;
import com.lightcare.server.profile.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 账号系统核心服务。
 *
 * - register: 校验手机号唯一 → bcrypt 哈希密码 → save user → 自动建默认 SELF profile（应用 Mifflin-St Jeor 算目标值）
 *             → 签 JWT 返 AuthRes（含 profile，注册完直接进主页）
 * - login: 查 user → bcrypt 校验 → 签 JWT 返 AuthRes（profile=null，client 跳 ProfileSelectionScreen 选/建档案）
 * - me: 返当前 userId / phone / nickname（用于 token 续期 / 客户端刷新本地缓存）
 *
 * 错误：手机号已注册 → PHONE_ALREADY_REGISTERED；密码错 → INVALID_CREDENTIAL。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepo;
    private final ProfileRepository profileRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthRes register(RegisterReq req) {
        if (userRepo.findByPhone(req.phone()).isPresent()) {
            throw new ApiException(ApiError.PHONE_ALREADY_REGISTERED);
        }
        UserEntity u = new UserEntity();
        u.setPhone(req.phone().trim());
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        u.setNickname(req.displayName().trim());
        u = userRepo.save(u);

        // 自动建一份默认 SELF 档案，应用身体数据算目标值
        ProfileEntity p = new ProfileEntity();
        p.setOwnerUserId(u.getId());
        p.setDisplayName(req.displayName().trim());
        p.setRelation(ProfileEntity.Relation.SELF);
        p.setHeightCm(req.heightCm());
        p.setWeightKg(req.weightKg());
        p.setActivityLevel(parseActivity(req.activityLevel()));
        ProfileTargetCalculator.apply(p);
        profileRepo.save(p);

        String token = jwtUtil.issue(u.getId(), u.getPhone());
        log.info("[AUTH] register uid={} phone={} profile={}", u.getId(), u.getPhone(), p.getId());
        return new AuthRes(u.getId(), u.getPhone(), u.getNickname(), token, toProfileDto(p));
    }

    public AuthRes login(LoginReq req) {
        UserEntity u = userRepo.findByPhone(req.phone().trim())
            .orElseThrow(() -> new ApiException(ApiError.INVALID_CREDENTIAL, "手机号或密码错误"));
        if (u.getPasswordHash() == null || !passwordEncoder.matches(req.password(), u.getPasswordHash())) {
            throw new ApiException(ApiError.INVALID_CREDENTIAL, "手机号或密码错误");
        }
        String token = jwtUtil.issue(u.getId(), u.getPhone());
        log.info("[AUTH] login uid={} phone={}", u.getId(), u.getPhone());
        // login 不返 profile（profile 列表由 GET /v1/profiles 拉）
        return new AuthRes(u.getId(), u.getPhone(), u.getNickname(), token, null);
    }

    public AuthMeRes me(long userId) {
        UserEntity u = userRepo.findById(userId)
            .orElseThrow(() -> new ApiException(ApiError.NOT_FOUND, "用户不存在"));
        return new AuthMeRes(u.getId(), u.getPhone(), u.getNickname());
    }

    /** GET /v1/profiles 复用的归属查询（暴露给 AuthController / ProfileController 都可）。 */
    public List<ProfileEntity> listProfiles(long userId) {
        return profileRepo.findByOwnerUserId(userId);
    }

    private static ProfileEntity.ActivityLevel parseActivity(String s) {
        if (s == null || s.isBlank()) return ProfileEntity.ActivityLevel.LIGHT;
        try { return ProfileEntity.ActivityLevel.valueOf(s.trim().toUpperCase()); }
        catch (Exception e) { return ProfileEntity.ActivityLevel.LIGHT; }
    }

    private static AuthRes.ProfileDto toProfileDto(ProfileEntity p) {
        return new AuthRes.ProfileDto(
            p.getId(),
            p.getOwnerUserId(),
            p.getManagedByUserId(),
            p.getDisplayName(),
            p.getAvatarUrl(),
            p.getRelation() == null ? "SELF" : p.getRelation().name(),
            p.getBirthDate() == null ? null : p.getBirthDate().toString(),
            p.getGender(),
            p.getHeightCm(),
            p.getWeightKg(),
            p.getActivityLevel() == null ? "LIGHT" : p.getActivityLevel().name(),
            p.getProteinTargetG(),
            p.getVegTargetServings(),
            p.getWaterTargetMl(),
            p.getStepTarget(),
            p.getCalorieTargetKcal()
        );
    }
}