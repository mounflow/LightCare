package com.lightcare.app.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightcare.app.data.repo.MealRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * PR3: 拍照后异步识别轮询。
 *
 * 持有方：MainActivity（ActivityScope），跨 Log/Home 切换不被销毁。
 * 流程：
 *   1) LogViewModel.submitPhotoEdit 拿到 mealId + status=PENDING → startPolling(mealId)
 *   2) 每 3 秒 GET /v1/meals/{id} 一次，最多 10 次（30s）
 *   3) 看到 status=DONE/FAILED 时停轮询 + 调 onComplete
 *   4) MainActivity 在 onComplete 里 refreshKey++ → HomeViewModel 重算余量（带 M3 精确值）
 *
 * 为什么抽这个 VM：LogVM 销毁后轮询不中断。
 */
@HiltViewModel
class RecognitionViewModel @Inject constructor(
    private val mealRepo: MealRepository
) : ViewModel() {

    data class UiState(
        val pendingMealId: Long? = null,
        val lastStatus: String? = null,
        val polls: Int = 0
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var pollJob: Job? = null

    /**
     * 开始轮询。onComplete 在 status=DONE/FAILED 或 30s 超时时触发。
     * 多次调用会取消上一次（同一 mealId 或新 mealId 都重置）。
     */
    /**
     * 开始轮询。onComplete 携带最终 status（DONE/FAILED/PENDING-超时），UI 据 status 决定是否提示失败。
     */
    fun startPolling(mealId: Long, onComplete: (String) -> Unit) {
        pollJob?.cancel()
        _state.value = UiState(pendingMealId = mealId, lastStatus = "PENDING", polls = 0)
        pollJob = viewModelScope.launch {
            repeat(MAX_POLLS) { idx ->
                delay(POLL_INTERVAL_MS)
                val meal = mealRepo.getById(mealId)
                val status = meal?.recognitionStatus ?: "PENDING"
                _state.value = _state.value.copy(lastStatus = status, polls = idx + 1)
                if (status == "DONE" || status == "FAILED") {
                    onComplete(status)
                    return@launch
                }
            }
            // 30s 超时：强制回调（哪怕还在 PENDING 也让 UI 停 spinner）
            onComplete("PENDING")
        }
    }

    /** 显式取消（用户在识别完成前退出页面，无需继续轮询）。 */
    fun cancel() {
        pollJob?.cancel()
        pollJob = null
        _state.value = UiState()
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
    }

    companion object {
        const val POLL_INTERVAL_MS = 3000L
        const val MAX_POLLS = 10
    }
}
