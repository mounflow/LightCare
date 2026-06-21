package com.lightcare.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightcare.app.data.api.LoginReq
import com.lightcare.app.data.api.RegisterReq
import com.lightcare.app.data.auth.AuthException
import com.lightcare.app.data.auth.AuthRepository
import com.lightcare.app.data.auth.AuthStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * PR-Auth：登录 / 注册共享 ViewModel。
 *
 * 注册只需 手机号 + 密码 + 称呼；身高体重在"我的身体数据"里填，server 会按 Mifflin-St Jeor 重算目标值。
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo: AuthRepository,
    private val store: AuthStore
) : ViewModel() {

    enum class Mode { LOGIN, REGISTER }

    data class UiState(
        val mode: Mode = Mode.LOGIN,
        val phone: String = "",
        val password: String = "",
        val displayName: String = "",
        /** 密码明文可见切换（注册屏 / 登录屏都可点） */
        val passwordVisible: Boolean = false,
        val loading: Boolean = false,
        val error: String? = null,
        /** 登录 / 注册成功 → MainActivity 据此切到 ProfileSelection 或直接进主页 */
        val success: Boolean = false,
        /** 进入页面时回填上次登录的手机号（登录屏） */
        val prefilledPhone: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        // 冷启动：上次登录的手机号回填到登录页（仅 LOGIN 模式）
        viewModelScope.launch {
            val saved = store.phone()
            if (!saved.isNullOrBlank()) {
                _state.value = _state.value.copy(prefilledPhone = saved, phone = saved)
            }
        }
    }

    fun setMode(mode: Mode) {
        _state.value = _state.value.copy(mode = mode, error = null)
    }

    fun updatePhone(v: String)   { _state.value = _state.value.copy(phone = v.filter { it.isDigit() }.take(11), error = null) }
    fun updatePassword(v: String) { _state.value = _state.value.copy(password = v.take(32), error = null) }
    fun updateName(v: String)    { _state.value = _state.value.copy(displayName = v.take(20), error = null) }
    fun togglePasswordVisible()  { _state.value = _state.value.copy(passwordVisible = !_state.value.passwordVisible) }

    fun submit() {
        val s = _state.value
        if (!validate(s)) return
        _state.value = s.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                when (s.mode) {
                    Mode.LOGIN -> repo.login(LoginReq(phone = s.phone, password = s.password))
                    Mode.REGISTER -> repo.register(
                        RegisterReq(
                            phone = s.phone,
                            password = s.password,
                            displayName = s.displayName.trim().ifBlank { "我" }
                        )
                    )
                }
                _state.value = _state.value.copy(loading = false, success = true)
            } catch (e: AuthException) {
                _state.value = _state.value.copy(loading = false, error = friendly(e.code, e.message ?: ""))
            } catch (t: Throwable) {
                _state.value = _state.value.copy(loading = false, error = "网络异常：${t.message ?: "请检查连接"}")
            }
        }
    }

    private fun validate(s: UiState): Boolean {
        if (s.phone.length != 11) {
            _state.value = s.copy(error = "请输入 11 位手机号")
            return false
        }
        if (s.password.length < 6) {
            _state.value = s.copy(error = "密码至少 6 位")
            return false
        }
        if (s.mode == Mode.REGISTER && s.displayName.isBlank()) {
            _state.value = s.copy(error = "请填写称呼")
            return false
        }
        return true
    }

    private fun friendly(code: Int, fallback: String): String = when (code) {
        40900 -> "该手机号已注册，直接登录即可"
        40101 -> "手机号或密码错误"
        40100 -> "登录已过期，请重新登录"
        else -> fallback.ifBlank { "请求失败（$code）" }
    }
}