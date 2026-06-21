package com.lightcare.server.auth.dto;

/** GET /v1/auth/me：当前 token 用户的基础信息（用于 token 续期时校验 server 侧还活着）。 */
public record AuthMeRes(long userId, String phone, String nickname) {}