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
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 选档 / 建档 ViewModel（本地化，不依赖登录）。
 *
 * 进入时拉档案列表：
 * - 空 → UI 引导"创建第一份档案" → [createFirst] → 写 AuthStore → 进主页。
 * - 非空 → 列档点选 → [select] → 写 AuthStore → 进主页。
 */
@HiltViewModel
class ProfileSelectionViewModel @Inject constructor(
    private val repo: ProfileRepository,
    private val authStore: AuthStore
) : ViewModel() {

    data class UiState(
        val loading: Boolean = false,
        val profiles: List<ProfileDto> = emptyList(),
        val busy: Boolean = false,
        val error: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        // 首次进入（userId 为 null）不调 list（避免无 token 401）；
        // 切档场景（profileId 已清空但 userId 仍在）才调 list 显示已有档案。
        viewModelScope.launch {
            if (authStore.userId() != null) load()
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            val list = repo.list()
            _state.value = UiState(loading = false, profiles = list ?: emptyList(),
                error = if (list == null) "无法连接到本地服务，请确认后端已启动" else null)
        }
    }

    /** 选中已有档案。成功后由 Activity 观察 AuthStore 变化自动跳主页。 */
    fun select(profile: ProfileDto) {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, error = null)
            try {
                repo.select(profile)
                // 成功：busy 清掉（profileIdFlow 跳转，不靠 busy 卡 UI）
                _state.value = _state.value.copy(busy = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(busy = false, error = "切换失败，请重试")
            }
        }
    }

    /** 创建第一份（或新增）档案。成功后写入 AuthStore，自动跳主页。 */
    fun createFirst(
        displayName: String,
        birthDate: String? = null,
        gender: String? = null,
        heightCm: Int? = null,
        weightKg: Double? = null,
        activityLevel: String? = null
    ) {
        if (_state.value.busy) return
        val name = displayName.trim().ifBlank { "我" }
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, error = null)
            val p = repo.bootstrap(
                displayName = name,
                birthDate = birthDate,
                gender = gender,
                heightCm = heightCm,
                weightKg = weightKg,
                activityLevel = activityLevel
            )
            // 无论成败都清 busy：成功靠 profileIdFlow 跳转，失败靠 error 提示。
            // 避免跳转有延迟时按钮持续转圈。
            _state.value = if (p == null) {
                _state.value.copy(busy = false, error = "建档失败，请确认本地服务已启动")
            } else {
                _state.value.copy(busy = false)
            }
        }
    }
}
