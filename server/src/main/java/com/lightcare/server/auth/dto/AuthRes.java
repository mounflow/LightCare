package com.lightcare.server.auth.dto;

/**
 * 注册 / 登录响应。
 *
 * - token = JWT（client 存 DataStore，Authorization: Bearer 携带）
 * - profile = register 时 server 自动建的默认 SELF 档案；login 时为 null（client 跳选档页）
 *   - 客户端体验：注册完直接进主页；登录完先 ProfileSelectionScreen 选/建档案
 */
public record AuthRes(
    long userId,
    String phone,
    String nickname,
    String token,
    /** register 返；login 返 null。ProfileDto 字段与 Android ProfileDto 完全对齐。 */
    ProfileDto profile
) {
    /**
     * 内嵌 DTO（避免循环依赖 server/ProfileController）。
     * 营养目标（蛋白/饮水/步数/卡路里）允许 null —— 用户没填身高体重时为 null，
     * 前端据此显示"—" + 引导去"我的身体数据"补全（不写假数据）。
     */
    public record ProfileDto(
        long id,
        long ownerUserId,
        Long managedByUserId,
        String displayName,
        String avatarUrl,
        String relation,
        String birthDate,
        String gender,
        Integer heightCm,
        Double weightKg,
        String activityLevel,
        Integer proteinTargetG,
        int vegTargetServings,
        Integer waterTargetMl,
        Integer stepTarget,
        Integer calorieTargetKcal
    ) {}
}