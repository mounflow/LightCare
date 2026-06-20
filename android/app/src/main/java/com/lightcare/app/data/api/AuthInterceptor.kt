package com.lightcare.app.data.api

import com.lightcare.app.data.auth.AuthStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 本地化：注入 X-LightCare-User-Id 头（= 建档时的 ownerUserId）。
 * 本地运行无需鉴权，后端 CurrentUserResolver 读此头做归属校验。
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val authStore: AuthStore
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val builder = req.newBuilder()
        val uid = runBlocking { authStore.userId() }
        if (uid != null) builder.header("X-LightCare-User-Id", uid.toString())
        return chain.proceed(builder.build())
    }
}
