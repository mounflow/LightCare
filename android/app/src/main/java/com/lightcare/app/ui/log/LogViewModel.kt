package com.lightcare.app.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightcare.app.data.api.CreateMealReq
import com.lightcare.app.data.api.LightCareApi
import com.lightcare.app.data.api.RecognizedItem
import com.lightcare.app.data.auth.AuthStore
import com.lightcare.app.data.food.FoodItem
import com.lightcare.app.data.food.FoodLibrary
import com.lightcare.app.data.food.FoodLibraryRepository
import com.lightcare.app.data.food.Multiplied
import com.lightcare.app.data.repo.MealRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

/**
 * 记录页 VM（本地化）。
 *
 * 闭环（PR2 拍照即入列）：
 *   拍照/相册 → startPhotoEdit(file, initial) → 弹 EditPhotoSheet
 *   用户编辑名称/分类/重量 → submitPhotoEdit() → 走 MealRepository.create
 *   onClose() → MainActivity refreshKey++ → Home 今日余量更新。
 *
 * 食物库手选（FoodPickerSheet → addDetected）路径保留不动。
 *
 * PR3: submitPhotoEdit 改为 multipart 走 POST /v1/meals/photo，触发 server 异步识别。
 */
data class LogUiState(
    val detected: List<FoodItem> = emptyList(),
    val slot: MealSlot = MealSlot.fromCurrentTime(),
    val portion: Portion = Portion.MEDIUM,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
    // PR2: 编辑照片食材
    val editingPhotoFile: File? = null,
    val editableItems: List<EditableFood> = emptyList(),
    val submittingPhoto: Boolean = false,
    // 详情页信息流卡片用：地点（自动获取，可改）+ 描述（用户手填心情）
    val location: String = "",
    val description: String = "",
    val locating: Boolean = false,
    // 旧识别（兼容，可能还在用）
    val recognizing: Boolean = false,
    val recognized: List<RecognizedItem> = emptyList(),
    val totalKcal: Int = 0,
    val totalProtein: Double = 0.0,
    val totalFat: Double = 0.0,
    val totalCarb: Double = 0.0,
    val totalWater: Int = 0
)

/** 编辑面板里的一行食材。weightG 是用户实际看到/改的数字。 */
data class EditableFood(
    val id: String = "edit_${System.nanoTime()}",
    var name: String,
    var category: String,
    var weightG: Int,
    var waterMl: Int = 0
)

@HiltViewModel
class LogViewModel @Inject constructor(
    private val repo: MealRepository,
    private val authStore: AuthStore,
    private val api: LightCareApi,
    val foodRepo: FoodLibraryRepository,
    private val locationFetcher: com.lightcare.app.data.location.LocationFetcher
) : ViewModel() {

    private val _state = MutableStateFlow(LogUiState())
    val state: StateFlow<LogUiState> = _state.asStateFlow()

    /** 用户从食物库手选若干食物加入本次记录。 */
    fun addDetected(items: List<FoodItem>) {
        _state.update { s ->
            val existing = s.detected.map { it.key }.toSet()
            val merged = s.detected + items.filter { it.key !in existing }
            s.copy(detected = merged).withTotals()
        }
    }

    fun removeDetected(item: FoodItem) {
        _state.update {
            it.copy(detected = it.detected.filterNot { x -> x.key == item.key }).withTotals()
        }
    }

    fun setPortion(p: Portion) = _state.update { it.copy(portion = p).withTotals() }
    fun setSlot(s: MealSlot) = _state.update { it.copy(slot = s) }

    // ────────────────────────── PR2: 编辑照片 ──────────────────────────

    /**
     * 进入"拍照编辑"模式。
     * @param file 压缩后的 jpg（cacheDir/rec_xxx.jpg），提交时随 multipart 上传
     * @param initial M3 同步识别预填项（PR2 阶段为空 list → 用户手填；PR3 阶段填 M3 识别的）
     */
    fun startPhotoEdit(file: File, initial: List<RecognizedItem> = emptyList()) {
        val items = if (initial.isEmpty()) {
            // PR2: 没 M3 预填，给一个空白行让用户开始填
            listOf(blankEditable())
        } else {
            initial.map { ri -> EditableFood(
                name = ri.name,
                category = if (ri.category.isBlank()) "其他" else ri.category,
                weightG = ri.weightG.coerceAtLeast(1),
                waterMl = ri.waterMl.coerceAtLeast(0)
            ) }
        }
        _state.update { it.copy(
            editingPhotoFile = file,
            editableItems = items,
            location = "",
            description = "",
            error = null
        ) }
        // 后台异步获取地点（3s 超时，拿不到留空）
        fetchLocation()
    }

    /** 尝试获取当前地点填入 state（拿不到静默留空，不阻塞用户）。 */
    fun fetchLocation() {
        if (_state.value.locating) return
        _state.update { it.copy(locating = true) }
        viewModelScope.launch {
            val place = try { locationFetcher.currentPlace() } catch (_: Exception) { null }
            _state.update { it.copy(locating = false, location = place ?: _state.value.location) }
        }
    }

    fun updateLocation(v: String) = _state.update { it.copy(location = v) }
    fun updateDescription(v: String) = _state.update { it.copy(description = v) }

    fun updateEditableName(index: Int, name: String) = _state.update { s ->
        s.copy(editableItems = s.editableItems.mapIndexed { i, e -> if (i == index) e.copy(name = name) else e })
    }

    fun updateEditableCategory(index: Int, category: String) = _state.update { s ->
        s.copy(editableItems = s.editableItems.mapIndexed { i, e -> if (i == index) e.copy(category = category) else e })
    }

    /** 重量变更（克）。 */
    fun updateEditableWeight(index: Int, weightG: Int) = _state.update { s ->
        val clamped = weightG.coerceAtLeast(1)
        s.copy(editableItems = s.editableItems.mapIndexed { i, e -> if (i == index) e.copy(weightG = clamped) else e })
    }

    fun removeEditable(index: Int) = _state.update { s ->
        val next = s.editableItems.toMutableList().also { if (it.size > 1) it.removeAt(index) }
        s.copy(editableItems = next)
    }

    fun addBlankEditable() = _state.update { s ->
        s.copy(editableItems = s.editableItems + blankEditable())
    }

    fun dismissEdit() = _state.update { it.copy(
        editingPhotoFile = null,
        editableItems = emptyList(),
        submittingPhoto = false
    ) }

    /**
     * 提交编辑结果。
     * PR3: 走 multipart POST /v1/meals/photo + 触发 server 异步识别 + 客户端轮询。
     *
     * 营养估算策略（粗估给"立即可见"用）：逐项 weightG × 食物库该 category 默认宏量聚合。
     * 真实 M3 精确值由 server 异步写回（识别完成时由 RecognitionViewModel 通知 MainActivity refreshKey++）。
     */
    fun submitPhotoEdit() {
        val s = _state.value
        val file = s.editingPhotoFile ?: return
        val items = s.editableItems.filter { it.name.isNotBlank() && it.weightG > 0 }
        if (items.isEmpty()) {
            _state.update { it.copy(error = "至少添加一项食材") }
            return
        }
        _state.update { it.copy(submittingPhoto = true, error = null) }
        viewModelScope.launch {
            val profileId = authStore.profileId() ?: run {
                _state.update { it.copy(submittingPhoto = false, error = "未选择档案") }
                return@launch
            }
            val summary = items.joinToString(" + ") { "${it.name}(${it.weightG}g)" }
            val previewJson = buildPreviewJson(items)
            val saved = repo.createFromPhoto(
                profileId = profileId,
                slot = s.slot.apiValue,
                portion = s.portion.apiValue,
                summary = summary,
                location = s.location,
                description = s.description,
                previewItemsJson = previewJson,
                imageFile = file
            )
            if (saved == null) {
                _state.update { it.copy(submittingPhoto = false, error = "记录失败，请稍后重试") }
                return@launch
            }
            // 立即关 sheet（余量会显示 PENDING 的基础值）
            _state.update { it.copy(submittingPhoto = false, saved = true) }
            // 通知 UI 层去触发轮询（ActivityScope 跨页面）
            onPendingMeal?.invoke(saved.id)
        }
    }

    /** PR3: RecognitionViewModel 轮询开始的回调（由 LogScreen 注入）。 */
    var onPendingMeal: ((Long) -> Unit)? = null

    private fun buildPreviewJson(items: List<EditableFood>): String {
        val list = items.joinToString(",") { e ->
            val name = e.name.replace("\"", "\\\"")
            "{\"name\":\"$name\",\"category\":\"${e.category}\",\"weightG\":${e.weightG}}"
        }
        return "[$list]"
    }

    private fun blankEditable() = EditableFood(name = "", category = "主食", weightG = 100)

    /**
     * 把「食物库手选」路径的 detected list 序列化成 items JSON。
     * 每项带 category + weightG + 该项的宏量分摊（kcal/protein/fat/carb/water），
     * 让 HomeViewModel 端能按 item × category 拆 segment 做按食物本身分类染色。
     */
    private fun buildItemsJsonFromLibrary(items: List<FoodItem>, portion: String): String {
        if (items.isEmpty()) return "[]"
        val list = items.joinToString(",") { f ->
            val m = FoodLibrary.multiply(f, portion)
            val name = f.displayName.replace("\"", "\\\"")
            val cat = f.category.ifBlank { "其他" }.replace("\"", "\\\"")
            // FoodItem 没存 weightG 字段（食物库以"一份"为基准），按约定 100g 标
            "{\"name\":\"$name\",\"category\":\"$cat\",\"weightG\":100," +
                "\"kcal\":${m.kcal},\"proteinG\":${m.protein},\"fatG\":${m.fat}," +
                "\"carbG\":${m.carb},\"waterMl\":${m.water}}"
        }
        return "[$list]"
    }


    // ────────────────────────── 食物库手选路径（保存） ──────────────────────────

    fun save() {
        val s = _state.value
        if (s.detected.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            try {
                val profileId = authStore.profileId()
                if (profileId == null) {
                    _state.update { it.copy(saving = false, error = "未选择档案") }
                    return@launch
                }
                val req = CreateMealReq(
                    profileId = profileId,
                    slot = s.slot.apiValue,
                    portion = s.portion.apiValue,
                    source = LogSource.MANUAL.apiValue,
                    summary = s.detected.joinToString(" + ") { it.displayName },
                    kcal = s.totalKcal,
                    proteinG = s.totalProtein,
                    fatG = s.totalFat,
                    carbG = s.totalCarb,
                    vegServings = s.detected.sumOf {
                        FoodLibrary.multiply(it, s.portion.apiValue).veg
                    },
                    waterMl = s.totalWater,
                    itemsJson = buildItemsJsonFromLibrary(s.detected, s.portion.apiValue)
                )
                val saved = repo.create(req)
                if (saved == null) {
                    _state.update { it.copy(saving = false, error = "记录失败，请稍后重试") }
                } else {
                    _state.update { it.copy(saving = false, saved = true) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(saving = false, error = "记录失败：${e.message ?: "未知错误"}") }
            }
        }
    }

    fun reset() {
        _state.value = LogUiState(slot = MealSlot.fromCurrentTime())
    }

    fun clearError() = _state.update { it.copy(error = null) }

    /** detected/portion 变化时重算营养合计（真实累加，非写死占位）。 */
    private fun LogUiState.withTotals(): LogUiState {
        var kcal = 0; var protein = 0.0; var fat = 0.0; var carb = 0.0; var water = 0
        detected.forEach { item ->
            val m: Multiplied = FoodLibrary.multiply(item, portion.apiValue)
            kcal += m.kcal
            protein += m.protein
            fat += m.fat
            carb += m.carb
            water += m.water
        }
        return copy(totalKcal = kcal, totalProtein = protein, totalFat = fat, totalCarb = carb, totalWater = water)
    }
}