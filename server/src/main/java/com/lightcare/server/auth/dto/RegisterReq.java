package com.lightcare.server.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 注册请求。手机号 + 密码 + 昵称；选填身高/体重（Mifflin-St Jeor 重算目标值）。
 */
public record RegisterReq(
    @NotBlank @Size(min = 11, max = 32) String phone,
    @NotBlank @Size(min = 8, max = 64) String password,
    @NotBlank @Size(max = 64) String displayName,
    Integer heightCm,
    Double weightKg,
    /** SEDENTARY / LIGHT / MODERATE / ACTIVE / VERY_ACTIVE；默认 LIGHT */
    String activityLevel
) {}