package com.lightcare.server.auth;

import com.lightcare.server.auth.dto.AuthMeRes;
import com.lightcare.server.auth.dto.AuthRes;
import com.lightcare.server.auth.dto.LoginReq;
import com.lightcare.server.auth.dto.RegisterReq;
import com.lightcare.server.common.ApiResponse;
import com.lightcare.server.common.CurrentUserAnnotation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 账号系统入口（公开）。
 *
 * - POST /v1/auth/register  → 返 token + userId + 默认 profile（自动建档）
 * - POST /v1/auth/login     → 返 token + userId（profile=null，跳选档页）
 * - GET  /v1/auth/me        → 当前 token 用户信息（鉴权）
 * - POST /v1/auth/logout    → 占位：当前 stateless，client 直接清本地 token；返 200 让 client 知道 server 没意见
 */
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<AuthRes> register(@Valid @RequestBody RegisterReq req) {
        return ApiResponse.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ApiResponse<AuthRes> login(@Valid @RequestBody LoginReq req) {
        return ApiResponse.ok(authService.login(req));
    }

    @GetMapping("/me")
    public ApiResponse<AuthMeRes> me(@CurrentUserAnnotation long userId) {
        return ApiResponse.ok(authService.me(userId));
    }

    /**
     * 占位登出：服务端 stateless，token 撤销在家庭场景下不必要（短 TTL 即可）。
     * 返 200 让 client 拿到响应后清本地 token / Room 缓存。
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.ok(null);
    }
}