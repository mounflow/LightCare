package com.lightcare.app.ui.food

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightcare.app.data.food.FoodItem
import com.lightcare.app.data.food.FoodLibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 食物库管理 VM：列出内置 + 自定义。
 * P39：详情编辑（update）+ 多选删除（selectionMode + deleteSelected）。
 */
@HiltViewModel
class FoodLibraryViewModel @Inject constructor(
    private val repo: FoodLibraryRepository
) : ViewModel() {

    data class UiState(
        val loading: Boolean = false,
        val defaults: List<FoodItem> = emptyList(),
        val customs: List<FoodItem> = emptyList(),
        val message: String? = null,
        /** P39：多选模式 + 已选中的 customId 集合（内置不可选/不可删） */
        val selectionMode: Boolean = false,
        val selectedCustomIds: Set<Long> = emptySet()
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            val all = repo.allItems()
            _state.value = _state.value.copy(
                loading = false,
                defaults = all.filter { it.isDefault },
                customs = all.filter { !it.isDefault }
            )
        }
    }

    /** 新增自定义食物。失败用 AddResult 区分原因（A9 文案准确）。 */
    fun add(input: com.lightcare.app.data.food.FoodLibraryRepository.AddFoodInput) {
        viewModelScope.launch {
            val result = repo.addCustom(input)
            _state.value = _state.value.copy(
                message = when (result) {
                    com.lightcare.app.data.food.FoodLibraryRepository.AddResult.Success -> "已添加"
                    com.lightcare.app.data.food.FoodLibraryRepository.AddResult.EmptyName -> "名称不能为空"
                    com.lightcare.app.data.food.FoodLibraryRepository.AddResult.DuplicateName -> "已存在同名食物"
                }
            )
            if (result == com.lightcare.app.data.food.FoodLibraryRepository.AddResult.Success) load()
        }
    }

    /** P39：更新已有自定义食物（按 id 修改 name/category/nutrition）。 */
    fun update(customId: Long, input: com.lightcare.app.data.food.FoodLibraryRepository.AddFoodInput) {
        viewModelScope.launch {
            val ok = repo.updateCustom(customId, input)
            _state.value = _state.value.copy(
                message = if (ok) "已保存修改" else "保存失败（名称可能重复）"
            )
            if (ok) load()
        }
    }

    /** P41：编辑内置项时调用 —— 以同名覆盖方式落 DB（不影响内置表）。 */
    fun overrideDefault(original: FoodItem, input: com.lightcare.app.data.food.FoodLibraryRepository.AddFoodInput) {
        viewModelScope.launch {
            val ok = repo.overrideDefault(original, input)
            _state.value = _state.value.copy(
                message = if (ok) "已覆盖内置项（${input.displayName}）" else "保存失败"
            )
            if (ok) load()
        }
    }

    /** 删除单个（P40 之前的旧入口，留作兼容）。 */
    fun delete(item: FoodItem) {
        val id = item.customId ?: return
        viewModelScope.launch {
            repo.deleteCustom(id)
            load()
        }
    }

    // === P39 多选删除 ===

    /** 长按一项 → 进入多选模式 + 自动选中该项 */
    fun enterSelection(initialItem: FoodItem) {
        val id = initialItem.customId ?: return  // 内置不可选
        _state.update { it.copy(selectionMode = true, selectedCustomIds = setOf(id)) }
    }

    /** 在多选模式下点击一项 → 切换选中 */
    fun toggleSelect(item: FoodItem) {
        val id = item.customId ?: return
        _state.update {
            val next = if (id in it.selectedCustomIds)
                it.selectedCustomIds - id else it.selectedCustomIds + id
            it.copy(selectedCustomIds = next)
        }
    }

    /** 全选所有自定义 */
    fun selectAll() {
        _state.update { it.copy(selectedCustomIds = it.customs.mapNotNull { c -> c.customId }.toSet()) }
    }

    /** 退出多选模式（清空选中） */
    fun exitSelection() {
        _state.update { it.copy(selectionMode = false, selectedCustomIds = emptySet()) }
    }

    /** 删除当前选中的所有食物（必须在多选模式下调用） */
    fun deleteSelected() {
        val ids = _state.value.selectedCustomIds
        if (ids.isEmpty()) return
        viewModelScope.launch {
            // P2-5: 并行删除（之前 forEach 串行，选 20 条要等 20 个 RTT）
            coroutineScope {
                ids.map { id -> async { repo.deleteCustom(id) } }.awaitAll()
            }
            _state.update {
                it.copy(
                    message = "已删除 ${ids.size} 项",
                    selectionMode = false,
                    selectedCustomIds = emptySet()
                )
            }
            load()
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }

    /** P47: 清空所有自定义食物（保留内置 22 条）。 */
    fun clearAllCustom() {
        viewModelScope.launch {
            repo.clearAllCustom()
            _state.update { it.copy(message = "已清空所有自定义食物", selectionMode = false, selectedCustomIds = emptySet()) }
            load()
        }
    }
}