package com.lightcare.app.ui.food

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightcare.app.data.api.FoodDto
import com.lightcare.app.data.api.LightCareApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * PR-D: 拍照自动入库冲突处理。
 *
 * 流程：拍照识别完 → MainActivity 在 RecognitionViewModel.onComplete 里调 [checkConflicts]
 *   → 拉到 PENDING_CONFLICT 列表非空 → showSheet=true → FoodConflictSheet 弹出
 *   → 用户对每条点「换名 / 覆盖 / 跳过」→ 调 [resolve] → 列表清空后自动关 sheet。
 *
 * ActivityScope（NavGraph 顶级持有），跨页面存活。
 */
@HiltViewModel
class FoodConflictViewModel @Inject constructor(
    private val api: LightCareApi
) : ViewModel() {

    data class UiState(
        val showSheet: Boolean = false,
        val conflicts: List<FoodDto> = emptyList(),
        val resolving: Boolean = false
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** 拉冲突列表；非空则弹 sheet。 */
    fun checkConflicts() {
        viewModelScope.launch {
            try {
                val resp = api.listFoodConflicts()
                val list = resp.data.orEmpty()
                _state.update { it.copy(showSheet = list.isNotEmpty(), conflicts = list) }
            } catch (_: Exception) {
                // 拉冲突失败不影响主流程（用户下次拍照还会再拉）
            }
        }
    }

    /** 解决单条冲突。RENAME 需 newName。 */
    fun resolve(id: Long, action: String, newName: String? = null) {
        viewModelScope.launch {
            _state.update { it.copy(resolving = true) }
            try {
                api.resolveFood(id, action, newName)
            } catch (_: Exception) {}
            // 重新拉列表
            val resp = api.listFoodConflicts()
            val list = resp.data.orEmpty()
            _state.update { it.copy(
                resolving = false,
                conflicts = list,
                showSheet = list.isNotEmpty()
            ) }
        }
    }

    fun dismiss() {
        _state.update { it.copy(showSheet = false) }
    }
}
