package com.lightcare.app.data.api

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    val code: Int,
    val message: String?,
    val data: T?
)

/** 本地化建档请求（不依赖登录）。 */
@JsonClass(generateAdapter = true)
data class BootstrapReq(
    val displayName: String,
    val birthDate: String? = null,
    val gender: String? = null,
    val heightCm: Int? = null,
    val weightKg: Double? = null,
    val activityLevel: String? = null
)

/** 更新身体数据（Mifflin-St Jeor 重新算目标值） */
@JsonClass(generateAdapter = true)
data class UpdatePhysiqueReq(
    val birthDate: String? = null,
    val gender: String? = null,
    val heightCm: Int? = null,
    val weightKg: Double? = null,
    val activityLevel: String? = null
)

/** 本地化建档响应：含 userId（= ownerUserId，后续请求当 X-LightCare-User-Id 头）+ profile。 */
@JsonClass(generateAdapter = true)
data class BootstrapRes(
    val userId: Long,
    val profile: ProfileDto
)

@JsonClass(generateAdapter = true)
data class ProfileDto(
    val id: Long,
    val ownerUserId: Long,
    val managedByUserId: Long?,
    val displayName: String,
    val avatarUrl: String?,
    val relation: String,
    val birthDate: String?,
    val gender: String?,
    val heightCm: Int?,
    val weightKg: Double?,
    val activityLevel: String?,
    val proteinTargetG: Int,
    val vegTargetServings: Int,
    val waterTargetMl: Int,
    val stepTarget: Int,
    val calorieTargetKcal: Int
)

@JsonClass(generateAdapter = true)
data class CreateProfileReq(
    val displayName: String,
    val avatarUrl: String? = null,
    val relation: String = "SELF",
    val birthDate: String? = null,
    val gender: String? = null,
    val heightCm: Int? = null,
    val weightKg: Double? = null,
    val activityLevel: String? = null
)

@JsonClass(generateAdapter = true)
data class MealDto(
    val id: Long,
    val profileId: Long,
    val slot: String,
    val portion: String,
    val source: String,
    val summary: String,
    val kcal: Int,
    val proteinG: Double,
    val fatG: Double,
    val carbG: Double,
    val fiberG: Double,
    val vegServings: Int,
    val waterMl: Int = 0,
    val recognitionStatus: String = "DONE",
    val location: String? = null,
    val description: String? = null,
    /** 食材明细 JSON（按食物分类染色用），旧数据可空。 */
    val itemsJson: String? = null,
    val mealDate: String,
    val mealTime: String
)

@JsonClass(generateAdapter = true)
data class CreateMealReq(
    val profileId: Long,
    val slot: String,
    val portion: String,
    val source: String,
    val summary: String,
    val kcal: Int = 0,
    val proteinG: Double = 0.0,
    val fatG: Double = 0.0,
    val carbG: Double = 0.0,
    val fiberG: Double = 0.0,
    val vegServings: Int = 0,
    val waterMl: Int = 0,
    val location: String? = null,
    val description: String? = null,
    /** 食材明细 JSON（用于按食物分类染色），手动 / 食物库手选路径都填。 */
    val itemsJson: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateExerciseReq(
    val profileId: Long,
    val kind: String,
    val intensity: String,
    val durationMin: Int,
    val steps: Int = 0,
    val fatigue: Int = 1
)

@JsonClass(generateAdapter = true)
data class AddWaterReq(val profileId: Long, val cups: Int = 1)

/** P6 食物识别结果（与 server RecognizedItem 对齐，含精确宏量营养）。 */
@JsonClass(generateAdapter = true)
data class RecognizedItem(
    val name: String,
    val category: String = "其他",
    val weightG: Int = 0,
    val kcal: Int = 0,
    val proteinG: Double = 0.0,
    val fatG: Double = 0.0,
    val carbG: Double = 0.0,
    val fiberG: Double = 0.0,
    val sugarG: Double = 0.0,
    val sodiumMg: Int = 0,
    val waterMl: Int = 0,           // PR1: M3 估算含水量（毫升）
    val confidence: Double = 0.0
)

/** PR-D: 食物库条目（与 server FoodController.FoodDto 对齐）。 */
@JsonClass(generateAdapter = true)
data class FoodDto(
    val id: Long,
    val ownerUserId: Long? = null,
    val key: String,
    val displayName: String,
    val category: String = "其他",
    val source: String = "MANUAL",
    val perServingG: Int = 0,
    val kcal: Int = 0,
    val proteinG: Double = 0.0,
    val fatG: Double = 0.0,
    val carbG: Double = 0.0,
    val fiberG: Double = 0.0,
    val waterMl: Int = 0,
    val vegServings: Int = 0,
    val isDefault: Boolean = false,
    val conflictStatus: String = "RESOLVED"
)

/** PR-D: 新增/更新食物请求（与 server UpsertFoodReq 对齐）。 */
@JsonClass(generateAdapter = true)
data class UpsertFoodReq(
    val key: String? = null,
    val displayName: String,
    val category: String = "其他",
    val perServingG: Int = 0,
    val kcal: Int = 0,
    val proteinG: Double = 0.0,
    val fatG: Double = 0.0,
    val carbG: Double = 0.0,
    val fiberG: Double = 0.0,
    val waterMl: Int = 0,
    val vegServings: Int = 0
)
