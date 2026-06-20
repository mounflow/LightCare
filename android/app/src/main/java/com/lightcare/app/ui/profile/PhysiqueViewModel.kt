package com.lightcare.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightcare.app.data.api.ProfileDto
import com.lightcare.app.data.auth.AuthStore
import com.lightcare.app.data.repo.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 身体数据 VM：当前档案的 physique + 编辑保存。
 *
 * - profile 跟随 profileIdFlow 自动 reload
 * - [updatePhysique] 更新后 server 自动按 Mifflin-St Jeor 重算目标值
 * - 同时被 [SettingsScreen] 与 [PhysiqueScreen] 使用
 */
@HiltViewModel
class PhysiqueViewModel @Inject constructor(
    private val authStore: AuthStore,
    private val profileRepo: ProfileRepository
) : ViewModel() {

    data class UiState(
        val profile: ProfileDto? = null,
        val loading: Boolean = true,
        val error: Boolean = false,
        val saving: Boolean = false,
        val message: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            authStore.profileIdFlow.collect { pid ->
                if (pid == null) {
                    _state.update { it.copy(profile = null, loading = false) }
                } else {
                    reload(pid)
                }
            }
        }
    }

    /** P1-3: 重新拉取（空状态/错误态的重试按钮用）。pid 缺省从当前 profile 取。 */
    fun reload(pid: Long? = _state.value.profile?.id) {
        if (pid == null) { _state.update { it.copy(loading = false) }; return }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = false) }
            val list = profileRepo.list()
            val p = list?.firstOrNull { it.id == pid }
            _state.update {
                it.copy(profile = p, loading = false, error = p == null && list == null)
            }
        }
    }

    /** 更新身体数据。任一字段为 null 表示不传（server 保持原值）。 */
    fun updatePhysique(
        birthDate: String?,
        gender: String?,
        heightCm: Int?,
        weightKg: Double?,
        activityLevel: String?
    ) {
        val pid = _state.value.profile?.id ?: return
        viewModelScope.launch {
            _state.update { it.copy(saving = true, message = null) }
            val updated = profileRepo.updatePhysique(pid, birthDate, gender, heightCm, weightKg, activityLevel)
            _state.update {
                it.copy(
                    saving = false,
                    profile = updated ?: it.profile,
                    message = if (updated != null) "已保存，每日目标已重新计算" else "保存失败，请确认本地服务已启动"
                )
            }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}