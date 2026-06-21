package com.lightcare.server.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** 登录请求：手机号 + 密码。 */
public record LoginReq(
    @NotBlank String phone,
    @NotBlank String password
) {}