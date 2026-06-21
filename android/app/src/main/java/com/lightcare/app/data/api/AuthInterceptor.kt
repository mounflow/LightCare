package com.lightcare.app.data.api

import com.lightcare.app.data.auth.AuthStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 注入鉴权头：
 *   - 有 token → `Authorization: Bearer <token>`（主路径，PR-Auth）
 *   - 仅当有 userId 但无 token（dev fallback 兼容期）→ 也发 `X-LightCare-User-Id`，便于本地裸 header 调试
 *
 * 公开路径（/v1/auth/register, /v1/auth/login 等）即使带 token 也没问题：server JwtAuthFilter
 * 只校验 token 合法性，不会因为已登录而拒绝注册。
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val authStore: AuthStore
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val builder = req.newBuilder()
        val token = runBlocking { authStore.token() }
        if (!token.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $token")
        } else {
            // dev fallback：老 client/调试场景；正常登录后不会走这里
            val uid = runBlocking { authStore.userId() }
            if (uid != null) builder.header("X-LightCare-User-Id", uid.toString())
        }
        return chain.proceed(builder.build())
    }
}