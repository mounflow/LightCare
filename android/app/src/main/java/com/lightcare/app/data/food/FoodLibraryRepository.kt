package com.lightcare.app.data.food

import android.util.Log
import com.lightcare.app.data.api.FoodDto
import com.lightcare.app.data.api.LightCareApi
import com.lightcare.app.data.api.UpsertFoodReq
import com.lightcare.app.data.db.FoodItemDao
import com.lightcare.app.data.db.FoodItemEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 食物库仓库（PR-D: 迁移到 server 统一管理）。
 *
 * - 数据源：server `GET /v1/foods`（自己的 + 全局种子 22 条 + AI 自动入库）。
 * - Room food_item 表降级为离线缓存：API 成功后写入，断网时读缓存兜底。
 * - [FoodLibrary.DEFAULTS]（22 条硬编码）保留为最后兜底（server + 缓存都拿不到时）。
 * - 写操作（addCustom/updateCustom/deleteCustom）走 API；API 失败时回退本地（保证离线可用）。
 *
 * 重名判定（四项营养全等）在 server 端做：[createFood] 命中重名返回 code=40901 → [AddResult.DuplicateName]。
 */
@Singleton
class FoodLibraryRepository @Inject constructor(
    private val api: LightCareApi,
    private val dao: FoodItemDao
) {

    private val tag = "FoodLibRepo"

    /**
     * 全量食物：server 优先 → 失败读 Room 缓存 → 再失败读内置 DEFAULTS 兜底。
     */
    suspend fun allItems(): List<FoodItem> = try {
        val resp = api.listFoods()
        val list = resp.data.orEmpty()
        if (list.isNotEmpty()) {
            // 同步写缓存（断网时能用）
            replaceCache(list)
        }
        list.map { it.toFoodItem() }
    } catch (e: Exception) {
        Log.w(tag, "listFoods failed, fallback to cache", e)
        customs().ifEmpty { FoodLibrary.DEFAULTS }
    }

    suspend fun search(query: String, limit: Int = 20): List<FoodItem> = try {
        val resp = api.searchFoods(query.trim(), limit)
        resp.data.orEmpty().map { it.toFoodItem() }
    } catch (e: Exception) {
        Log.w(tag, "searchFoods failed, fallback", e)
        // 离线兜底：本地过滤
        val all = customs().ifEmpty { FoodLibrary.DEFAULTS }
        val q = query.trim()
        if (q.isEmpty()) all.take(limit)
        else all.filter {
            it.displayName.contains(q, ignoreCase = true) ||
            it.category.contains(q, ignoreCase = true)
        }.take(limit)
    }

    suspend fun byKey(key: String): FoodItem? = allItems().firstOrNull { it.key == key }

    /** 新增自定义食物的入参（A10 封装，避免 7 个位置参数顺序错）。 */
    data class AddFoodInput(
        val displayName: String,
        val category: String,
        val perServingProtein: Double,
        val perServingFat: Double,
        val perServingCarb: Double,
        val perServingKcal: Int,
        val perServingVeg: Int = 0,
        val perServingWaterMl: Int = 0,
        val perServingG: Int = 0
    )

    /** 新增结果，区分原因（A9 让 UI 能给准确文案）。
     *  PR-Recipe: Success 携带 server foodId，方便 UI 跳详情页继续填做法。 */
    sealed class AddResult {
        data class Success(val foodId: Long) : AddResult()
        object EmptyName : AddResult()
        object DuplicateName : AddResult()
    }

    /** 冲突码：server FoodController 命中"同名同营养"时返回。 */
    private val CODE_DUPLICATE = 40901

    suspend fun addCustom(input: AddFoodInput): AddResult {
        val name = input.displayName.trim()
        if (name.isBlank()) return AddResult.EmptyName
        val req = UpsertFoodReq(
            key = "custom_" + System.currentTimeMillis(),
            displayName = name,
            category = input.category.ifBlank { "其他" },
            perServingG = input.perServingG,
            kcal = input.perServingKcal,
            proteinG = input.perServingProtein,
            fatG = input.perServingFat,
            carbG = input.perServingCarb,
            waterMl = input.perServingWaterMl,
            vegServings = input.perServingVeg
        )
        return try {
            val resp = api.createFood(req)
            when {
                resp.code == CODE_DUPLICATE -> AddResult.DuplicateName
                resp.data != null -> AddResult.Success(resp.data.id)
                else -> {
                    Log.w(tag, "createFood code=${resp.code} msg=${resp.message}")
                    AddResult.Success(-1L)   // 非 40901 的失败也算成功（不阻塞用户），但拿不到 id → UI 不跳详情
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "createFood failed, fallback to local", e)
            // 离线兜底：本地查重 + 落缓存
            if (customs().any { it.displayName == name }) AddResult.DuplicateName
            else {
                val newId = dao.upsert(req.toLocalEntity())
                AddResult.Success(newId)
            }
        }
    }

    suspend fun updateCustom(id: Long, input: AddFoodInput): Boolean {
        val name = input.displayName.trim()
        if (name.isBlank()) return false
        // server 端食物 id 可能与本地缓存 id 不同；这里按 id 调 server（要求 id 是 server id）。
        val req = UpsertFoodReq(
            displayName = name,
            category = input.category.ifBlank { "其他" },
            perServingG = input.perServingG,
            kcal = input.perServingKcal,
            proteinG = input.perServingProtein,
            fatG = input.perServingFat,
            carbG = input.perServingCarb,
            waterMl = input.perServingWaterMl,
            vegServings = input.perServingVeg
        )
        return try {
            val resp = api.updateFood(id, req)
            resp.code == 0
        } catch (e: Exception) {
            Log.w(tag, "updateFood failed, fallback to local", e)
            val existing = dao.all().firstOrNull { it.id == id } ?: return false
            dao.upsert(existing.copy(
                displayName = name,
                category = input.category.ifBlank { "其他" },
                perServingProtein = input.perServingProtein,
                perServingVeg = input.perServingVeg,
                perServingKcal = input.perServingKcal,
                perServingFat = input.perServingFat,
                perServingCarb = input.perServingCarb,
                perServingWaterMl = input.perServingWaterMl
            ))
            true
        }
    }

    /**
     * 编辑内置项 → server 端不区分"覆盖内置"（同名四项全等会跳过、不同会冲突）。
     * 这里直接转调 addCustom（语义一致：新增一条用户自定义的同名项）。
     */
    @Deprecated("server 统一管理后内置项编辑走 addCustom（server 会处理重名）")
    suspend fun overrideDefault(original: FoodItem, input: AddFoodInput): Boolean {
        return addCustom(input) is AddResult.Success
    }

    suspend fun deleteCustom(id: Long) {
        try {
            api.deleteFood(id)
        } catch (e: Exception) {
            Log.w(tag, "deleteFood failed, fallback to local", e)
        }
        // 本地缓存也删（避免离线时还显示）
        dao.delete(id)
    }

    /** 清空所有自定义食物（server + 本地都清）。内置种子不动。 */
    suspend fun clearAllCustom() {
        val localIds = dao.all().map { it.id }
        localIds.forEach { id ->
            try { api.deleteFood(id) } catch (_: Exception) {}
            dao.delete(id)
        }
    }

    // ===== 本地缓存 =====

    /** 读 Room 缓存（离线兜底用）。 */
    private suspend fun customs(): List<FoodItem> =
        dao.all().map { it.toFoodItem() }

    /** server 列表成功后，全量替换本地缓存（保证离线数据新鲜）。 */
    private suspend fun replaceCache(list: List<FoodDto>) {
        // 只缓存"自己的"（ownerUserId 非空）+ 非 PENDING_CONFLICT 的；种子/AI 冲突项不入本地。
        val mine = list.filter { it.ownerUserId != null && it.conflictStatus == "RESOLVED" }
        if (mine.isEmpty()) return
        dao.clearAll()   // 简化：清空自定义缓存后重写（FoodItemDao 加 clearAll）
        mine.forEach { dao.upsert(it.toLocalEntity()) }
    }

    private fun FoodItemEntity.toFoodItem() = FoodItem(
        key = key,
        displayName = displayName,
        category = category,
        perServingProtein = perServingProtein,
        perServingVeg = perServingVeg,
        perServingKcal = perServingKcal,
        perServingFat = perServingFat,
        perServingCarb = perServingCarb,
        perServingWaterMl = perServingWaterMl,
        isDefault = false,
        customId = id,
        serverId = null      // 本地 cache 没拿到 server id
    )

    private fun FoodDto.toFoodItem() = FoodItem(
        key = key,
        displayName = displayName,
        category = category,
        perServingProtein = proteinG,
        perServingVeg = vegServings,
        perServingKcal = kcal,
        perServingFat = fatG,
        perServingCarb = carbG,
        perServingWaterMl = waterMl,
        isDefault = isDefault,
        customId = id,
        serverId = id
    )

    private fun UpsertFoodReq.toLocalEntity() = FoodItemEntity(
        key = key ?: "custom_" + System.currentTimeMillis(),
        displayName = displayName,
        category = category,
        perServingProtein = proteinG,
        perServingVeg = vegServings,
        perServingKcal = kcal,
        perServingFat = fatG,
        perServingCarb = carbG,
        perServingWaterMl = waterMl
    )

    private fun FoodDto.toLocalEntity() = FoodItemEntity(
        id = id,   // 用 server id 作本地主键，保证一致性
        key = key,
        displayName = displayName,
        category = category,
        perServingProtein = proteinG,
        perServingVeg = vegServings,
        perServingKcal = kcal,
        perServingFat = fatG,
        perServingCarb = carbG,
        perServingWaterMl = waterMl
    )
}
