package com.lightcare.app.data.api

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RecommendCardDto(
    val id: Long,
    val kind: String,
    val status: String,
    val title: String,
    val body: String,
    /** PR-Recipe: meal 类卡关联的 foodId（exercise 类为 null）；点推荐卡跳详情页用。 */
    val foodId: Long? = null
)

@JsonClass(generateAdapter = true)
data class ExerciseRecReq(
    val profileId: Long,
    val fatigue: Int,
    val stepsToday: Int
)

@JsonClass(generateAdapter = true)
data class WeeklyReportDto(
    val weekStart: String,
    val weekEnd: String,
    val daysLogged: Int,
    val mealCount: Int,
    val nutrition: NutritionDto,
    val praise: String,
    val highlights: List<HighlightDto>
)

@JsonClass(generateAdapter = true)
data class NutritionDto(
    val proteinPct: Int,
    val vegPct: Int,
    val waterPct: Int,
    val kcalPct: Int = 0
)

@JsonClass(generateAdapter = true)
data class HighlightDto(
    val emoji: String,
    val title: String,
    val body: String
)
