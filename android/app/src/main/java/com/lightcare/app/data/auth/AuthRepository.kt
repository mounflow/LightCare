package com.lightcare.app.data.auth

import com.lightcare.app.data.api.ApiResponse
import com.lightcare.app.data.api.AuthMeRes
import com.lightcare.app.data.api.AuthRes
import com.lightcare.app.data.api.LightCareApi
import com.lightcare.app.data.api.LoginReq
import com.lightcare.app.data.api.RegisterReq
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PR-Auth：账号系统仓储。
 *
 * 错误约定：
 *  - 成功：返回 Result.success(AuthRes)
 *  - 失败：throw AuthException(code, message) —— UI 层 catch 后弹 Snackbar / 红字提示
 *  - 网络/解析异常：抛原异常（IOException / HttpException 等）
 */
@Singleton
class AuthRepository @Inject constructor(
    private val api: LightCareApi,
    private val authStore: AuthStore
) {

    suspend fun register(req: RegisterReq): AuthRes {
        val res = unwrap(api.register(req))
        authStore.saveSession(res.userId, res.phone, res.nickname, res.token)
        // 注册成功 server 自动建默认 SELF profile，直接持久化避免走选档页
        res.profile?.let { authStore.saveProfile(it.id, it.displayName) }
        return res
    }

    suspend fun login(req: LoginReq): AuthRes {
        val res = unwrap(api.login(req))
        authStore.saveSession(res.userId, res.phone, res.nickname, res.token)
        // 登录不写 profileId —— 让 UI 跳到 ProfileSelectionScreen 选档 / 建档
        return res
    }

    /** 校验 server 侧还活着（用于冷启动时确认 token 未失效）。 */
    suspend fun me(): AuthMeRes = unwrap(api.me())

    /** 退出登录：通知 server（best-effort）+ 清本地。 */
    suspend fun logout() {
        runCatching { api.logout() }   // 401 也无所谓，反正本地要清
        authStore.clear()
    }

    private fun <T> unwrap(res: ApiResponse<T>): T {
        if (res.code == 0) {
            @Suppress("UNCHECKED_CAST")
            return res.data as T
        }
        throw AuthException(res.code, res.message ?: "未知错误")
    }
}

class AuthException(val code: Int, message: String) : RuntimeException(message)