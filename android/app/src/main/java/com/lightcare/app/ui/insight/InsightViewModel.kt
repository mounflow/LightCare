package com.lightcare.app.ui.insight

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightcare.app.data.api.LightCareApi
import com.lightcare.app.data.api.WeeklyReportDto
import com.lightcare.app.data.auth.AuthStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InsightUiState(
    val loading: Boolean = true,
    val report: WeeklyReportDto? = null,
    val error: String? = null
)

@HiltViewModel
class InsightViewModel @Inject constructor(
    private val api: LightCareApi,
    private val authStore: AuthStore
) : ViewModel() {

    private val _state = MutableStateFlow(InsightUiState())
    val state: StateFlow<InsightUiState> = _state.asStateFlow()

    init {
        // 切档后自动重新加载。
        viewModelScope.launch {
            authStore.profileIdFlow.collect { if (it != null) load() }
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val pid = authStore.profileId()
                if (pid == null) {
                    // A4 修复：空档必须显式复位 loading，否则永久转圈。
                    _state.update { it.copy(loading = false, error = "未选择档案") }
                    return@launch
                }
                Log.i("InsightVM", "weeklyReport req profileId=$pid")
                val resp = api.weeklyReport(pid)
                Log.i("InsightVM", "weeklyReport resp code=${resp.code} data=${resp.data != null}")
                _state.update { it.copy(loading = false, report = resp.data) }
            } catch (e: Exception) {
                Log.w("InsightVM", "weeklyReport FAILED", e)
                _state.update { it.copy(loading = false, error = "加载周报失败：${e.javaClass.simpleName}: ${e.message ?: "未知"}") }
            }
        }
    }
}
