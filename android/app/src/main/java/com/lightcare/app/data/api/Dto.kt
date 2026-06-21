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
    /**
     * 营养目标值 —— 用户还没填身高体重/年龄时为 null。
     * 前端据此显示"—" + 引导去"我的身体数据"补全（不展示假数据）。
     */
    val proteinTargetG: Int?,
    val vegTargetServings: Int,
    val waterTargetMl: Int?,
    val stepTarget: Int?,
    val calorieTargetKcal: Int?
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
    val confidence: Double = 0.0,
    val recipe: RecipeHintDto? = null   // PR-Recipe: M3 顺便输出的做法提示（旧识别结果无此字段，默 null）
)

/** PR-Recipe: 识别时 M3 返回的做法提示（轻量版，不持久化）。 */
@JsonClass(generateAdapter = true)
data class RecipeHintDto(
    val cookingMinutes: Int = 0,
    val difficulty: String? = null,
    val ingredients: List<RecipeItemDto> = emptyList(),
    val seasonings: List<RecipeItemDto> = emptyList(),
    val steps: List<RecipeStepDto> = emptyList()
)

/** PR-Recipe: 食材 / 调料一行（用量字符串，如 "2 个" / "100g"）。 */
@JsonClass(generateAdapter = true)
data class RecipeItemDto(val name: String = "", val amount: String = "")

/** PR-Recipe: 做法一步。order 给 1-based 排序用，text 是中文描述。 */
@JsonClass(generateAdapter = true)
data class RecipeStepDto(val order: Int = 0, val text: String = "")

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

/** PR-Recipe: 食物完整做法（GET /v1/foods/{id}/recipe 返回）。 */
@JsonClass(generateAdapter = true)
data class RecipeDto(
    val foodId: Long,
    val cookingMinutes: Int = 0,
    val difficulty: String = "EASY",
    val ingredients: List<RecipeItemDto> = emptyList(),
    val seasonings: List<RecipeItemDto> = emptyList(),
    val steps: List<RecipeStepDto> = emptyList(),
    /** MANUAL = 用户手填；AI = M3 自动生成。 */
    val source: String = "MANUAL"
) {
    /** 是否值得展示做法区块（M3 没把握 / 用户没填都走"无做法"空状态）。 */
    val isEmpty: Boolean
        get() = cookingMinutes <= 0
            && ingredients.isEmpty()
            && seasonings.isEmpty()
            && steps.isEmpty()
}

/** PR-Recipe: upsert 请求。null = "不修改"，空 list = "清空"。 */
@JsonClass(generateAdapter = true)
data class UpsertRecipeReq(
    val cookingMinutes: Int? = null,
    val difficulty: String? = null,
    val ingredients: List<RecipeItemDto>? = null,
    val seasonings: List<RecipeItemDto>? = null,
    val steps: List<RecipeStepDto>? = null,
    val source: String? = null
)

// ===== PR-Auth: 账号系统（手机号 + 密码 + JWT）=====

/** 注册请求：必填 phone + password + displayName；身高体重可空（注册时填了 server 用来算默认目标值）。 */
@JsonClass(generateAdapter = true)
data class RegisterReq(
    val phone: String,
    val password: String,
    val displayName: String,
    val heightCm: Int? = null,
    val weightKg: Double? = null
)

/** 登录请求：手机号 + 密码。 */
@JsonClass(generateAdapter = true)
data class LoginReq(
    val phone: String,
    val password: String
)

/** 注册 / 登录响应：userId + phone + nickname + token + 可选 profile（注册时 server 自动建默认 SELF profile）。 */
@JsonClass(generateAdapter = true)
data class AuthRes(
    val userId: Long,
    val phone: String,
    val nickname: String,
    val token: String,
    val profile: ProfileDto? = null
)

/** /v1/auth/me 响应：仅基本信息（不带 token / profile）。 */
@JsonClass(generateAdapter = true)
data class AuthMeRes(
    val userId: Long,
    val phone: String,
    val nickname: String
)
