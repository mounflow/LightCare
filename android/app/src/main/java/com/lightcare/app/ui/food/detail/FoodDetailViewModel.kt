package com.lightcare.app.ui.food.detail

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightcare.app.data.api.FoodDto
import com.lightcare.app.data.api.LightCareApi
import com.lightcare.app.data.api.RecipeDto
import com.lightcare.app.data.recipe.RecipeRepository
import com.lightcare.app.data.recipe.RecipeRepository.UpsertResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 食物详情（PR-Recipe）ViewModel。
 *
 * 职责：
 * - 接受导航参数 [FOOD_ID_KEY]，从食物库列表中按 id 找到对应 FoodDto（无需新增 GET 端点）。
 * - 并行拉一份 recipe；recipe 为 null 表示"无做法"，走空状态 CTA，不报错。
 * - [upsertRecipe] 成功后刷新本地 state，让父 UI 自动反映。
 *
 * 三态：loading / loaded / error。loaded 内额外区分 hasRecipe / noRecipe。
 */
@HiltViewModel
class FoodDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val api: LightCareApi,
    private val recipeRepo: RecipeRepository
) : ViewModel() {

    private val foodId: Long = savedStateHandle.get<Long>(FOOD_ID_KEY) ?: -1L

    private val _state = MutableStateFlow(FoodDetailUiState())
    val state: StateFlow<FoodDetailUiState> = _state.asStateFlow()

    fun load() {
        if (foodId <= 0) {
            _state.update { it.copy(loading = false, error = "无效的食物 id") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, saveMessage = null) }
            try {
                // 食物基础信息从食物库列表里查（避免新增 GET 端点）。
                val food = api.listFoods().data.orEmpty().firstOrNull { it.id == foodId }
                if (food == null) {
                    _state.update { it.copy(loading = false, error = "找不到该食物（可能已被删除）") }
                    return@launch
                }
                val recipe = recipeRepo.getRecipe(foodId)   // null = 无做法，不算错
                _state.update {
                    it.copy(loading = false, food = food, recipe = recipe, error = null)
                }
            } catch (e: Exception) {
                Log.w(TAG, "load failed", e)
                _state.update {
                    it.copy(loading = false, error = "加载失败：${e.message ?: e.javaClass.simpleName}")
                }
            }
        }
    }

    /**
     * 提交做法编辑。保存成功后用返回的最新 RecipeDto 替换本地 recipe，触发 UI 自动重绘。
     */
    fun upsertRecipe(req: com.lightcare.app.data.api.UpsertRecipeReq) {
        viewModelScope.launch {
            _state.update { it.copy(saving = true, saveMessage = null) }
            when (val result = recipeRepo.upsertRecipe(foodId, req)) {
                is UpsertResult.Success -> _state.update {
                    it.copy(saving = false, recipe = result.recipe, saveMessage = "已保存")
                }
                UpsertResult.Forbidden -> _state.update {
                    it.copy(saving = false, saveMessage = "种子食物暂不可编辑")
                }
                is UpsertResult.Error -> _state.update {
                    it.copy(saving = false, saveMessage = result.message)
                }
            }
        }
    }

    /** 关闭顶部 "已保存" 提示条（用户手动 dismiss）。 */
    fun clearSaveMessage() {
        _state.update { it.copy(saveMessage = null) }
    }

    companion object {
        const val FOOD_ID_KEY = "foodId"
        private const val TAG = "FoodDetailVM"
    }
}

data class FoodDetailUiState(
    val loading: Boolean = true,
    val food: FoodDto? = null,
    val recipe: RecipeDto? = null,
    val saving: Boolean = false,
    /** 一次性提示（"已保存" / "种子食物暂不可编辑" / 错误）。 */
    val saveMessage: String? = null,
    val error: String? = null
)