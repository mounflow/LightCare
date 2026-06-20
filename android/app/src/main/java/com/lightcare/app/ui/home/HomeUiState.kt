package com.lightcare.app.ui.home

import java.time.LocalDate

/**
 * 首页 Dashboard 状态（温暖圆润版）。
 * 字段：周/月柱状图 + 今日余量 + 推荐食谱。
 */
data class HomeUiState(
    // 进度区视图状态
    val view: ProgressView = ProgressView.WEEK,
    /** 当前周的 ISO 周号（1-based，年内） */
    val weekNumber: Int = LocalDate.now().get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear()),
    /** 周/月区间显示文本（"第 25 周 · 06-15~21" 或 "6 月 · 6-01~6-30"） */
    val rangeText: String = "",
    /** 当前视图的柱状图（周=7 天，月=4-6 周列；日柱高=0..1） */
    val bars: List<DayBar> = emptyList(),
    /** 今日（视图内）哪一柱要高亮（0-based index） */
    val activeBarIndex: Int = 0,

    // 今日余量（剩多少）—— 由 HomeViewModel.refresh 按已记录餐次更新
    val remnants: List<NutrientRemnant> = emptyList(),
    /** 当前档案名（顶栏显示，多档案场景知道看的是谁）。 */
    val currentProfileName: String = "",

    /** 今日各餐次的营养贡献（点营养条某段弹卡时用）。 */
    val slotContribs: List<MealSlotContribution> = emptyList(),

    // 推荐 Top 5（P44：基于余量缺口 + 食物库候选 + 优先级打分）
    val recommendations: List<RecommendedFood> = emptyList()
)

/** 进度区视图模式 */
enum class ProgressView { WEEK, MONTH }

/**
 * 柱状图单柱。
 * - height: 0..1 归一化柱高（绘图用，分母 = max(本周最大kcal, targetKcal)）
 * - kcal / targetKcal / isOverTarget: PR-B 颜色编码 + 超标告警用
 */
data class DayBar(
    val label: String,
    val height: Float,
    val kcal: Int = 0,
    val targetKcal: Int = 0,
    val isOverTarget: Boolean = false
)

data class NutrientRemnant(
    val name: String,
    /** 该营养项的目标值（如蛋白 60） */
    val target: Double,
    val unit: String,
    /** 已摄入总量（各餐次之和） */
    val totalConsumed: Double,
    /** 按餐次分段：每段 = 一顿在该营养项的占比（0..1，相对 target）+ 该段颜色/餐次 */
    val segments: List<NutrientSegment>
) {
    /** 已消耗占比（0..100），封顶 100。 */
    val consumedPct: Float
        get() = if (target <= 0) 0f else (totalConsumed * 100.0 / target).toFloat().coerceIn(0f, 100f)
    /** 显示文案：已摄入/目标（如「48 / 60」），达标显示「已达标 🎉」。 */
    val remain: String
        get() = if (consumedPct >= 100f) "已达标 🎉"
                else "${fmt(totalConsumed)} / ${fmt(target)}"
}

private fun fmt(v: Double): String =
    if (v >= 100) "${v.toInt()}" else "${"%.1f".format(v)}"

/**
 * 一项营养在某**餐次**上的占比段（胶囊按顿分段染色）。
 * 一段 = 一顿（早/午/晚/加餐）在该营养项的占比。
 * 点某段 → 弹那一顿的详情（MealSlotContribution）。
 * 这正是 PRD 要的「每顿不同颜色，点中那一段显示那一顿的蛋白/脂肪/碳水/水分详情」。
 */
data class NutrientSegment(
    /** 餐次：BREAKFAST/LUNCH/DINNER/SNACK */
    val slot: String,
    /** 中文餐次名（弹卡/图例用）：早餐/午餐/晚餐/加餐 */
    val slotDisplay: String,
    /** 该段颜色（按 slot 取，HomeViewModel 算好塞进来）。 */
    val color: Long,
    /** 该段占整个胶囊宽度的比例（0..1，相对 target；可 >1 超标时截断在渲染层）。 */
    val fraction: Float,
    /** 该顿在这个营养项的实际数值（弹卡显示用）。 */
    val value: Double,
    val unit: String
)

/**
 * 一餐的营养贡献（点营养条某段弹卡显示）。
 * 按"哪一顿吃了什么"展示：餐类 + 时间 + 四项营养 + 这顿的食物明细（来自 itemsJson）。
 */
data class MealSlotContribution(
    val slot: String,
    val slotDisplay: String,
    val color: Long,
    val time: String,           // 如 "08:24"
    val kcal: Int,
    val protein: Double,
    val fat: Double,
    val carb: Double,
    val water: Int,
    val summary: String,        // meal.summary（兜底文案）
    /** 这顿的食物明细（来自 itemsJson 解析；旧数据无则空，弹卡用 summary 兜底）。 */
    val foods: List<MealFoodLine> = emptyList()
)

/** 一顿里的单个食物明细（弹卡列出"这顿吃了什么"用）。 */
data class MealFoodLine(val name: String, val category: String, val kcal: Int)

/** P44 推荐食物卡（含排序分与理由）。 */
data class RecommendedFood(
    val key: String,
    val displayName: String,
    val category: String,
    val perServingKcal: Int,
    val perServingProtein: Double,
    val perServingFat: Double,
    val perServingCarb: Double,
    /** 排序分（0..1，越高越靠前）。 */
    val score: Double,
    /** 推荐理由（"补蛋白 60g"、"补碳水 80g" 等简短文案）。 */
    val reason: String
)
