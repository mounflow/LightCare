package com.lightcare.app.data.repo

import com.lightcare.app.data.api.CreateMealReq
import com.lightcare.app.data.api.LightCareApi
import com.lightcare.app.data.db.MealCacheDao
import com.lightcare.app.data.db.MealCacheEntity
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MealRepository @Inject constructor(
    private val api: LightCareApi,
    private val dao: MealCacheDao
) {
    suspend fun listByDate(profileId: Long, date: String): List<MealCacheEntity> {
        return try {
            val resp = api.listMeals(profileId, date)
            val items = resp.data.orEmpty().map { it.toCache() }
            // A11 修复：server 返回正 id 前，先清掉同 profile+date 的离线负 id，
            // 避免离线记录与在线记录并存导致色环双算。
            dao.deleteOfflineByDate(profileId, date)
            dao.upsertAll(items)
            items
        } catch (e: Exception) {
            // 离线兜底：读本地最近 3 天缓存
            dao.listByDate(profileId, date)
        }
    }

    /**
     * P38: 周/月视图一次拿范围内所有 meal（替代 N 次连续 GET）。
     * @param from/to ISO 日期 "yyyy-MM-dd"，from ≤ to
     * @return 按日期分组的 Map（dateString → 该日 meal 列表），失败时返回空 map
     */
    suspend fun range(profileId: Long, from: String, to: String): Map<String, List<MealCacheEntity>> {
        return try {
            val resp = api.listMealsRange(profileId, from, to)
            val items = resp.data.orEmpty().map { it.toCache() }
            // 补写 Room 缓存：让最新营养（识别完回写的精确值）回写本地，断网时余量不读旧值
            if (items.isNotEmpty()) dao.upsertAll(items)
            items.groupBy { it.mealDate }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    suspend fun create(req: CreateMealReq): MealCacheEntity? {
        // 在线优先：成功后用服务端 id 写缓存
        return try {
            val resp = api.createMeal(req)
            val dto = resp.data ?: return null
            val entity = dto.toCache()
            dao.upsert(entity)
            entity
        } catch (e: Exception) {
            // 离线兜底：用本地负 id 落盘，保证记录不丢、首页色环可刷新
            val today = java.time.LocalDate.now().toString()
            val now = java.time.LocalTime.now().withNano(0).toString()
            val localId = -(System.currentTimeMillis() % Int.MAX_VALUE)
            val entity = MealCacheEntity(
                id = localId,
                profileId = req.profileId,
                slot = req.slot, portion = req.portion, source = req.source,
                summary = req.summary, kcal = req.kcal,
                proteinG = req.proteinG, fatG = req.fatG, carbG = req.carbG,
                fiberG = req.fiberG, vegServings = req.vegServings,
                waterMl = req.waterMl, recognitionStatus = "DONE",
                location = req.location ?: "", description = req.description ?: "",
                itemsJson = req.itemsJson ?: "",
                mealDate = today, mealTime = now
            )
            dao.upsert(entity)
            entity
        }
    }

    /**
     * PR3: 拍照即入列。
     * 上传 image + previewItems JSON → server 同步保存 (status=PENDING) + 异步识别 → 返回 mealId。
     * 失败（离线）返 null。
     */
    suspend fun createFromPhoto(
        profileId: Long,
        slot: String,
        portion: String,
        summary: String,
        location: String,
        description: String,
        previewItemsJson: String,
        imageFile: File
    ): MealCacheEntity? {
        return try {
            val text = "text/plain".toMediaTypeOrNull()
            val resp = api.createMealFromPhoto(
                profileId = profileId.toString().toRequestBody(text),
                slot = slot.toRequestBody(text),
                portion = portion.toRequestBody(text),
                summary = summary.toRequestBody(text),
                location = location.toRequestBody(text),
                description = description.toRequestBody(text),
                previewItems = previewItemsJson.toRequestBody(text),
                image = MultipartBody.Part.createFormData(
                    "image", imageFile.name, imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                )
            )
            val dto = resp.data
            if (dto == null) {
                android.util.Log.w("MealRepository", "createFromPhoto: response data null, code=${resp.code} msg=${resp.message}")
                return null
            }
            val entity = dto.toCache()
            dao.upsert(entity)
            entity
        } catch (e: Exception) {
            // 不要静默吞：拍照提交失败时打堆栈，让 LogScreen 的 error 提示有意义、logcat 能定位根因。
            android.util.Log.e("MealRepository", "createFromPhoto failed", e)
            null
        }
    }

    /**
     * PR3: 轮询单条 meal。识别完成时 RecognitionViewModel 看到 status=DONE/FAILED 停轮询。
     * 成功时同时 upsert 进 Room（让 HomeViewModel 拿最新值）。
     */
    suspend fun getById(id: Long): MealCacheEntity? {
        return try {
            val resp = api.getMeal(id)
            val dto = resp.data ?: return null
            val entity = dto.toCache()
            dao.upsert(entity)
            entity
        } catch (e: Exception) {
            null
        }
    }

    /** 删除一条 meal（server + 本地缓存）。P0-1: 详情页删除餐次用。 */
    suspend fun delete(id: Long): Boolean = try {
        api.deleteMeal(id)
        dao.delete(id)
        true
    } catch (e: Exception) {
        android.util.Log.e("MealRepository", "delete failed id=$id", e)
        false
    }
}

/** MealDto → Room 缓存实体（统一构造，避免 5 处重复 + 漏字段）。 */
private fun com.lightcare.app.data.api.MealDto.toCache(): MealCacheEntity = MealCacheEntity(
    id = id, profileId = profileId, slot = slot, portion = portion,
    source = source, summary = summary, kcal = kcal,
    proteinG = proteinG, fatG = fatG, carbG = carbG,
    fiberG = fiberG, vegServings = vegServings,
    waterMl = waterMl, recognitionStatus = recognitionStatus,
    location = location ?: "", description = description ?: "",
    itemsJson = itemsJson ?: "",
    mealDate = mealDate, mealTime = mealTime
)
