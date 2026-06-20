package com.lightcare.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightcare.app.data.auth.AuthStore
import com.lightcare.app.data.db.MealCacheEntity
import com.lightcare.app.data.repo.MealRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * 每餐记录时间线（详情页）。
 *
 * 默认展示「本周」（周一~今天）。按日期分组，每天一个 section，section 内按 mealTime 升序。
 * 每条 meal 提供图片 URL（baseUrl + /v1/meals/{id}/image），供 UI 缩略图加载。
 *
 * 不用 NavHost 传参：profileId 从 AuthStore 读（与 HomeViewModel 同模式）。
 */
@HiltViewModel
class MealHistoryViewModel @Inject constructor(
    private val mealRepo: MealRepository,
    private val authStore: AuthStore,
    val imageLoader: MealImageLoader
) : ViewModel() {

    /** P0-1: 删除一条餐次，删完重新加载列表。返回是否成功。 */
    fun deleteMeal(id: Long, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val ok = mealRepo.delete(id)
            if (ok) load()
            onResult(ok)
        }
    }

    /** 一天的记录 section。 */
    data class DaySection(
        val dateLabel: String,       // "06-20 周五" / "今天" 等
        val meals: List<MealCacheEntity>
    )

    data class UiState(
        val loading: Boolean = true,
        val sections: List<DaySection> = emptyList(),
        val rangeText: String = "",
        val weekOffset: Int = 0,    // P1-11: 0=本周，-1=上周，1=下周
        val canGoNext: Boolean = false,  // 不能超过本周（未来没数据）
        val error: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            authStore.profileIdFlow.collect { if (it != null) load() }
        }
    }

    /** P1-11: 切换到上一周。 */
    fun prevWeek() {
        _state.update { it.copy(weekOffset = it.weekOffset - 1, canGoNext = true) }
        load()
    }

    /** P1-11: 切换到下一周（不超过本周）。 */
    fun nextWeek() {
        if (_state.value.weekOffset >= 0) return
        val newOffset = _state.value.weekOffset + 1
        _state.update { it.copy(weekOffset = newOffset, canGoNext = newOffset < 0) }
        load()
    }

    fun load() {
        viewModelScope.launch {
            val pid = authStore.profileId() ?: run {
                _state.update { it.copy(loading = false, error = "未选择档案") }
                return@launch
            }
            _state.update { it.copy(loading = true, error = null) }
            try {
                val today = LocalDate.now()
                val offset = _state.value.weekOffset
                // P1-11: 按 weekOffset 平移本周的周一/周日
                val thisMonday = today.with(DayOfWeek.MONDAY)
                val monday = thisMonday.plusWeeks(offset.toLong())
                val sunday = monday.plusDays(6)
                val byDate = mealRepo.range(pid, monday.toString(), sunday.toString())
                val fmt = DateTimeFormatter.ofPattern("MM-dd")
                // 本周（offset==0）不显示未来的天；其它周显示完整 7 天
                val sections = (0..6).mapNotNull { i ->
                    val d = monday.plusDays(i.toLong())
                    if (offset == 0 && d.isAfter(today)) return@mapNotNull null
                    val list = byDate[d.toString()].orEmpty().sortedBy { it.mealTime }
                    if (list.isEmpty()) null else DaySection(dateLabelOf(d, today), list)
                }
                _state.update {
                    it.copy(
                        loading = false,
                        sections = sections,
                        rangeText = "${monday.format(fmt)} ~ ${sunday.format(fmt)}" +
                            if (offset == 0) "（本周）" else if (offset < 0) "（${-offset} 周前）" else ""
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = "加载失败，请稍后重试") }
            }
        }
    }
}

/** 日期标签：今天/昨天用相对，其它用 "MM-dd 周X"。 */
internal fun dateLabelOf(d: LocalDate, today: LocalDate): String {
    val dow = when (d.dayOfWeek) {
        DayOfWeek.MONDAY -> "周一"; DayOfWeek.TUESDAY -> "周二"; DayOfWeek.WEDNESDAY -> "周三"
        DayOfWeek.THURSDAY -> "周四"; DayOfWeek.FRIDAY -> "周五"; DayOfWeek.SATURDAY -> "周六"
        DayOfWeek.SUNDAY -> "周日"; else -> ""
    }
    val fmt = DateTimeFormatter.ofPattern("MM-dd")
    val prefix = when {
        d.isEqual(today) -> "今天"
        d.isEqual(today.minusDays(1)) -> "昨天"
        else -> d.format(fmt)
    }
    return "$prefix $dow"
}

/** slot 英文 → 中文 + emoji。 */
internal fun slotDisplay(slot: String): Pair<String, String> = when (slot) {
    "BREAKFAST" -> "早餐" to "🌅"
    "LUNCH" -> "午餐" to "☀️"
    "DINNER" -> "晚餐" to "🌙"
    "SNACK" -> "加餐" to "🍎"
    else -> "记录" to "🍽️"
}
