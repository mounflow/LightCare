package com.lightcare.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightcare.app.data.auth.AuthStore
import com.lightcare.app.data.api.ProfileDto
import com.lightcare.app.data.db.MealCacheEntity
import com.lightcare.app.data.food.FoodLibraryRepository
import com.lightcare.app.data.repo.MealRepository
import com.lightcare.app.data.repo.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val mealRepo: MealRepository,
    private val profileRepo: ProfileRepository,
    private val foodLib: FoodLibraryRepository,
    private val authStore: AuthStore
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        // 切档/建档后 profileId 变化 → 自动刷新。
        viewModelScope.launch {
            authStore.profileIdFlow.distinctUntilChanged().collect {
                if (it != null) refresh()
            }
        }
    }

    /** 周/月视图切换 */
    fun setView(v: ProgressView) {
        _state.update { it.copy(view = v) }
        refresh()
    }

    /** 刷新首页全部内容（余量 + 进度区柱状图）。 */
    fun refresh() {
        viewModelScope.launch {
            val profileId = authStore.profileId() ?: return@launch
            val today = LocalDate.now()
            val view = _state.value.view

            // P2-2: profile 只拉一次，传给三个方法复用（之前各调一次 listProfiles，共 3 次冗余）
            val profile = profileRepo.list()?.firstOrNull { it.id == profileId }

            // P2-3: 今日 meal + 范围 meal 并行拉（两个独立网络请求）
            val (todayMeals, byDate) = coroutineScope {
                val todayDeferred = async { mealRepo.listByDate(profileId, today.toString()) }
                val rangeDeferred = async {
                    // 范围按视图算（周/月）
                    if (view == ProgressView.WEEK) {
                        val monday = today.with(java.time.DayOfWeek.MONDAY)
                        val sunday = monday.plusDays(6)
                        mealRepo.range(profileId, monday.toString(), sunday.toString())
                    } else {
                        val firstMonday = today.withDayOfMonth(1).with(java.time.DayOfWeek.MONDAY)
                        mealRepo.range(profileId, firstMonday.toString(), today.withDayOfMonth(today.lengthOfMonth()).toString())
                    }
                }
                Pair(todayDeferred.await(), rangeDeferred.await())
            }

            updateRemnants(profile, todayMeals)
            buildRecommendations(profile, todayMeals)

            // 进度区柱状图（byDate 已并行拉好，直接聚合）
            val (bars, rangeText, activeIdx, weekNo) = when (view) {
                ProgressView.WEEK -> buildWeekBars(byDate, profile, today)
                ProgressView.MONTH -> buildMonthBars(byDate, profile, today)
            }
            _state.update {
                it.copy(
                    bars = bars,
                    rangeText = rangeText,
                    activeBarIndex = activeIdx,
                    weekNumber = weekNo,
                    currentProfileName = profile?.displayName ?: ""
                )
            }
        }
    }

    private suspend fun updateRemnants(profile: ProfileDto?, meals: List<MealCacheEntity>) {
        val proteinTarget = (profile?.proteinTargetG ?: 60).toDouble()
        val fatTarget = (proteinTarget * 0.7).coerceAtLeast(20.0)
        val carbTarget = (profile?.calorieTargetKcal?.let { it / 8 } ?: 200).toDouble()
        val waterTarget = (profile?.waterTargetMl ?: 1700).toDouble()

        // 按餐次分组（同 slot 的多顿合并，如两次加餐算一起）。按时序排：早<午<晚<加餐。
        val slotOrder = listOf("BREAKFAST", "LUNCH", "DINNER", "SNACK")
        val bySlot = meals.groupBy { it.slot }
            .toList()
            .sortedBy { (slot, _) -> slotOrder.indexOf(slot).let { if (it < 0) 99 else it } }

        // 每顿的完整四项营养贡献（弹卡用）+ 食物明细（弹卡列出"这顿吃了什么"）
        val slotContribs = bySlot.map { (slot, ms) ->
            // 这顿的食物明细：优先用 itemsJson 拆出的 item，没有就用 summary 兜底成一行
            val foods = ms.flatMap { parseItemsJsonForFoods(it.itemsJson, it.summary, it.kcal) }
            MealSlotContribution(
                slot = slot,
                slotDisplay = slotDisplay(slot),
                color = slotColor(slot),
                time = ms.minOf { it.mealTime }.take(5),
                kcal = ms.sumOf { it.kcal },
                protein = ms.sumOf { it.proteinG },
                fat = ms.sumOf { it.fatG },
                carb = ms.sumOf { it.carbG },
                water = ms.sumOf { it.waterMl },
                summary = ms.joinToString("、") { it.summary }.ifBlank { slotDisplay(slot) },
                foods = foods
            )
        }

        // PRD：胶囊按「餐次」分段（早/午/晚/加餐各一色），点段弹那一顿详情。
        // 每个营养项的 segment = 当天各顿在该营养项的占比。
        val slotAgg = bySlot.associate { (slot, ms) ->
            slot to SlotNutrition(
                slot = slot, slotDisplay = slotDisplay(slot), color = slotColor(slot),
                protein = ms.sumOf { it.proteinG }, fat = ms.sumOf { it.fatG },
                carb = ms.sumOf { it.carbG }, water = ms.sumOf { it.waterMl }.toDouble()
            )
        }
        val remnants = listOf(
            buildRemnant(slotAgg, slotOrder, "蛋白质", proteinTarget, "g") { it.protein },
            buildRemnant(slotAgg, slotOrder, "脂肪", fatTarget, "g") { it.fat },
            buildRemnant(slotAgg, slotOrder, "碳水", carbTarget, "g") { it.carb },
            buildRemnant(slotAgg, slotOrder, "水分", waterTarget, "ml") { it.water }
        )
        _state.update { it.copy(remnants = remnants, slotContribs = slotContribs) }
    }

    /** 一顿在四项营养上的聚合（构造 segment 用）。 */
    private data class SlotNutrition(
        val slot: String, val slotDisplay: String, val color: Long,
        val protein: Double, val fat: Double, val carb: Double, val water: Double
    )

    /** 一个营养项 → 按 slotOrder 顺序的 segment 列表。selector 从 SlotNutrition 取该营养项值。 */
    private fun buildRemnant(
        slotAgg: Map<String, SlotNutrition>, slotOrder: List<String>,
        name: String, target: Double, unit: String,
        selector: (SlotNutrition) -> Double
    ): NutrientRemnant {
        val total = slotAgg.values.sumOf(selector)
        val segments = slotOrder.mapNotNull { slot ->
            val sn = slotAgg[slot] ?: return@mapNotNull null
            val v = selector(sn)
            if (v <= 0) return@mapNotNull null   // 该顿在这个营养项为 0 不显示段
            val fraction = if (target <= 0) 0f else (v / target).toFloat()
            NutrientSegment(sn.slot, sn.slotDisplay, sn.color, fraction, v, unit)
        }
        return NutrientRemnant(name, target, unit, total, segments)
    }

    /**
     * 解析 meal.itemsJson 成弹卡用的食物明细行。
     * itemsJson 缺失/解析失败 → 用 summary 兜底成一行（保证弹卡至少能显示点东西）。
     */
    private fun parseItemsJsonForFoods(itemsJson: String, summary: String, mealKcal: Int): List<MealFoodLine> {
        val parsed = parseItemsJson(itemsJson)
        if (parsed.isNotEmpty()) {
            return parsed.map { MealFoodLine(it.name, it.category, 0) }
        }
        // 兜底：summary 形如 "米饭(150g) + 鸡腿(120g)"
        val fallbackName = summary.ifBlank { "这一餐" }
        return listOf(MealFoodLine(fallbackName, "其他", mealKcal))
    }

    // 拆 meal.itemsJson 时用的临时单 item 数据类（弹卡食物明细用）。
    private data class MealFoodItem(val name: String, val category: String)

    /**
     * 极简 JSON 解析（不引 moshi）：从 itemsJson 拆出食物名 + 分类，供弹卡展示"这顿吃了什么"。
     *  - 期望输入 [{"name":"...","category":"...",...}, ...]
     *  - 解析失败 → 返回空 list（外层用 summary 兜底）。
     */
    private fun parseItemsJson(json: String): List<MealFoodItem> {
        if (json.isBlank() || json == "[]") return emptyList()
        val out = mutableListOf<MealFoodItem>()
        val items = json.trim().removePrefix("[").removeSuffix("]").split("},{")
        for (raw in items) {
            val s = raw.trim().removePrefix("{").removeSuffix("}")
            val kv = parseFlatJsonObject(s) ?: return emptyList()
            val name = kv["name"].orEmpty()
            if (name.isBlank()) continue
            val category = kv["category"]?.ifBlank { "其他" } ?: "其他"
            out += MealFoodItem(name, category)
        }
        return out
    }

    /** 解析无嵌套、无转义引号、无数组的扁平 JSON object → key→rawValue 映射。 */
    private fun parseFlatJsonObject(body: String): Map<String, String>? {
        val result = linkedMapOf<String, String>()
        var i = 0
        while (i < body.length) {
            // 找下一个 key 的开引号
            val kStart = body.indexOf('"', i)
            if (kStart < 0) return result
            val kEnd = body.indexOf('"', kStart + 1)
            if (kEnd < 0) return null
            val key = body.substring(kStart + 1, kEnd)
            // 找 ":"
            val colon = body.indexOf(':', kEnd + 1)
            if (colon < 0) return null
            // 找 value（字符串 / 数字）
            i = colon + 1
            while (i < body.length && body[i].isWhitespace()) i++
            if (i >= body.length) return null
            val v: String
            if (body[i] == '"') {
                val vEnd = body.indexOf('"', i + 1)
                if (vEnd < 0) return null
                v = body.substring(i + 1, vEnd)
                i = vEnd + 1
            } else {
                // 数字或 true/false，直到遇到 , 或 }
                var j = i
                while (j < body.length && body[j] != ',' && body[j] != '}') j++
                v = body.substring(i, j).trim()
                i = j
            }
            result[key] = v
        }
        return result
    }

    /**
     * 餐次颜色（ARGB Long）—— 与 Color.kt 的 SlotBreakfast/Lunch/Dinner/Snack 保持一致，
     * 这样胶囊段色和图例圆点色完全统一。早=蜜桃粉 / 午=麦黄 / 晚=薰衣草 / 加餐=蜜橘。
     */
    private fun slotColor(slot: String): Long = when (slot) {
        "BREAKFAST" -> 0xFFFFB4A2
        "LUNCH"     -> 0xFFFFD68A
        "DINNER"    -> 0xFFB39DDB
        "SNACK"     -> 0xFFFFAB91
        else        -> 0xFF9CCC65   // 柠檬绿兜底
    }

    private fun slotDisplay(slot: String): String = when (slot) {
        "BREAKFAST" -> "早餐"; "LUNCH" -> "午餐"; "DINNER" -> "晚餐"; "SNACK" -> "加餐"; else -> "记录"
    }

    /**
     * P44 推荐算法：基于今日营养缺口，对 FoodLibraryRepository.allItems() 打分，取 Top 5。
     *
     * 目标值同 updateRemnants：protein=profile.proteinTargetG, fat=protein*0.7（下限 20）,
     * carb=calorieTargetKcal/8。缺口 = max(0, target - 已摄入)。打分公式：
     *   score = 0.5 * min(perServingProtein / gap_protein, 1)
     *         + 0.3 * min(perServingCarb   / gap_carb,   1)
     *         + 0.2 * min(perServingFat    / gap_fat,    1)
     * 缺口为 0 的项对应贡献记 0（已达标）。所有缺口都为 0 时 reason = "营养均衡之选"。
     */
    private suspend fun buildRecommendations(profile: ProfileDto?, meals: List<MealCacheEntity>) {
        val proteinConsumed = meals.sumOf { it.proteinG }
        val fatConsumed = meals.sumOf { it.fatG }
        val carbConsumed = meals.sumOf { it.carbG }

        val proteinTarget = profile?.proteinTargetG ?: 60
        val fatTarget = (proteinTarget * 0.7).toInt().coerceAtLeast(20)
        val carbTarget = profile?.calorieTargetKcal?.let { it / 8 } ?: 200

        val gapProtein = (proteinTarget - proteinConsumed).coerceAtLeast(0.0)
        val gapFat = (fatTarget - fatConsumed).coerceAtLeast(0.0)
        val gapCarb = (carbTarget - carbConsumed).coerceAtLeast(0.0)

        val allOnTarget = gapProtein <= 0.0 && gapFat <= 0.0 && gapCarb <= 0.0

        val scored = foodLib.allItems().map { item ->
            val proteinScore = if (gapProtein > 0.0)
                (item.perServingProtein / gapProtein).coerceAtMost(1.0) else 0.0
            val carbScore = if (gapCarb > 0.0)
                (item.perServingCarb / gapCarb).coerceAtMost(1.0) else 0.0
            val fatScore = if (gapFat > 0.0)
                (item.perServingFat / gapFat).coerceAtMost(1.0) else 0.0
            val total = 0.5 * proteinScore + 0.3 * carbScore + 0.2 * fatScore

            val reason = if (allOnTarget) {
                "营养均衡之选"
            } else {
                // 取该食物贡献最大的那个缺口（绝对克数）
                val candidates = listOf(
                    Triple(item.perServingProtein, gapProtein, "补蛋白"),
                    Triple(item.perServingCarb, gapCarb, "补碳水"),
                    Triple(item.perServingFat, gapFat, "补脂肪")
                ).filter { it.second > 0.0 && it.first > 0.0 }
                if (candidates.isEmpty()) {
                    "营养均衡之选"
                } else {
                    val best = candidates.maxBy { it.first }
                    "${best.third} ${formatGram(best.first)}"
                }
            }

            total to RecommendedFood(
                key = item.key,
                displayName = item.displayName,
                category = item.category,
                perServingKcal = item.perServingKcal,
                perServingProtein = item.perServingProtein,
                perServingFat = item.perServingFat,
                perServingCarb = item.perServingCarb,
                score = total,
                reason = reason
            )
        }.sortedByDescending { it.first }
            .take(5)
            .map { it.second }

        _state.update { it.copy(recommendations = scored) }
    }

    /** 克数格式化：整数显示整数，小数显示 1 位。 */
    private fun formatGram(v: Double): String =
        if (v >= 100.0) v.toInt().toString()
        else if (v == v.toInt().toDouble()) v.toInt().toString()
        else String.format("%.1f", v)


    /**
     * 周视图：本周 7 天（一 ~ 日），ISO 周（周一开始）。
     * P38 性能修：一次 range 调用拿到 7 天数据，本地按日期分组算柱高，避免 7 次连续 GET。
     */
    private suspend fun buildWeekBars(
        byDate: Map<String, List<MealCacheEntity>>, profile: ProfileDto?, today: LocalDate
    ): Quad<List<DayBar>, String, Int, Int> {
        val monday = today.with(DayOfWeek.MONDAY)
        val sunday = monday.plusDays(6)
        val targetKcal = profile?.calorieTargetKcal ?: 2000
        // PR-B: 柱高按当日 kcal（不再用餐次数）；颜色编码/超标由 DayBar.kcal + isOverTarget 承载。
        val raw = (0..6).map { i ->
            val date = monday.plusDays(i.toLong())
            val kcal = byDate[date.toString()]?.sumOf { it.kcal } ?: 0
            Triple(date, kcal, dayLabel(date.dayOfWeek))
        }
        // 归一化分母 = max(本周最大 kcal, targetKcal)：既保留"哪天吃最多"对比，又让 target 占稳定比例。
        val maxKcal = maxOf(raw.maxOf { it.second }, targetKcal, 1)
        val days = raw.map { (_, kcal, label) ->
            DayBar(
                label = label,
                height = (kcal.toFloat() / maxKcal).coerceIn(0f, 1f),
                kcal = kcal,
                targetKcal = targetKcal,
                isOverTarget = kcal > targetKcal
            )
        }
        val activeIdx = (today.dayOfWeek.value - 1)  // MONDAY=1 → 0
        val weekNo = today.get(WeekFields.ISO.weekOfWeekBasedYear())
        val fmt = DateTimeFormatter.ofPattern("MM-dd")
        val range = "${monday.format(fmt)}~${sunday.format(fmt)}"
        val text = "第 $weekNo 周 · $range"
        return Quad(days, text, activeIdx, weekNo)
    }

    /**
     * 月视图：当前月的 ISO 周分组（4-6 周），每柱代表 1 周。
     * P38 性能修：一次 range 调用拿到整月数据，本地按周聚合。
     */
    private suspend fun buildMonthBars(
        byDate: Map<String, List<MealCacheEntity>>, profile: ProfileDto?, today: LocalDate
    ): Quad<List<DayBar>, String, Int, Int> {
        val firstOfMonth = today.withDayOfMonth(1)
        val lastOfMonth = today.withDayOfMonth(today.lengthOfMonth())
        val firstMonday = firstOfMonth.with(DayOfWeek.MONDAY)
        val lastMondayOfMonth = lastOfMonth.with(DayOfWeek.MONDAY)
        // P1-12: 月视图目标用「日目标 × 7」（每周柱按周累计 kcal 比对）
        val targetKcal = (profile?.calorieTargetKcal ?: 2000) * 7
        val weeks = ((lastMondayOfMonth.toEpochDay() - firstMonday.toEpochDay()) / 7 + 1).toInt()
        val weeksInMonth = generateSequence(firstMonday) { it.plusDays(7) }.take(weeks).toList()
        var activeIdx = -1
        val rawKcal = weeksInMonth.mapIndexed { idx, weekStart ->
            val kcal = (0..6).sumOf { d ->
                val date = weekStart.plusDays(d.toLong())
                if (date.month == today.month) byDate[date.toString()]?.sumOf { it.kcal } ?: 0 else 0
            }
            if (weekStart <= today && today < weekStart.plusDays(7)) activeIdx = idx
            Triple(weekStart, kcal, "${weekStart.dayOfMonth}-${weekStart.plusDays(6).dayOfMonth}")
        }
        val maxKcal = maxOf(rawKcal.maxOf { it.second }, targetKcal, 1)
        val bars = rawKcal.map { (_, kcal, label) ->
            DayBar(
                label = label,
                height = (kcal.toFloat() / maxKcal).coerceIn(0f, 1f),
                kcal = kcal,
                targetKcal = targetKcal,
                isOverTarget = kcal > targetKcal
            )
        }
        val monthText = "${today.monthValue} 月"
        val weekNo = today.get(WeekFields.ISO.weekOfWeekBasedYear())
        return Quad(bars, monthText, activeIdx.coerceAtLeast(0), weekNo)
    }

    private fun dayLabel(d: DayOfWeek): String = when (d) {
        DayOfWeek.MONDAY -> "一"; DayOfWeek.TUESDAY -> "二"; DayOfWeek.WEDNESDAY -> "三"
        DayOfWeek.THURSDAY -> "四"; DayOfWeek.FRIDAY -> "五"; DayOfWeek.SATURDAY -> "六"
        DayOfWeek.SUNDAY -> "日"; else -> "?"
    }

    /**
     * 单项余量。PRD 风格原则：永远不展示负向差值 ——
     * 超出目标时 remain 显示"已达标 🎉"而非负数，consumedPct 封顶 100。
     * @param unit 显示单位（g / ml），PR1 加水分项需要 ml
     */
}

/** Kotlin stdlib 没 Quad，自定义 4 元组（data class 自动生成 component1-4） */
private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)