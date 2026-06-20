package com.lightcare.app.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lightcare.app.ui.theme.BorderSubtle
import com.lightcare.app.ui.theme.D
import com.lightcare.app.ui.theme.Error
import com.lightcare.app.ui.theme.LCAppear
import com.lightcare.app.ui.theme.LCCapsuleProgress
import com.lightcare.app.ui.theme.LCConcentricRings
import com.lightcare.app.ui.theme.LCEmojiBadge
import com.lightcare.app.ui.theme.LCSegmentedCapsule
import com.lightcare.app.ui.theme.Motion
import com.lightcare.app.ui.theme.NutrientProtein
import com.lightcare.app.ui.theme.NutrientVeg
import com.lightcare.app.ui.theme.NutrientWater
import com.lightcare.app.ui.theme.Outline
import com.lightcare.app.ui.theme.Primary
import com.lightcare.app.ui.theme.PrimaryContainer
import com.lightcare.app.ui.theme.RingSpec
import com.lightcare.app.ui.theme.S
import com.lightcare.app.ui.theme.SlotBreakfast
import com.lightcare.app.ui.theme.categoryEmojiOf
import com.lightcare.app.ui.theme.SlotDinner
import com.lightcare.app.ui.theme.SlotLunch
import com.lightcare.app.ui.theme.SlotSnack
import com.lightcare.app.ui.theme.SurgicalGreen
import com.lightcare.app.ui.theme.Warning
import com.lightcare.app.ui.theme.ambientCard
import androidx.compose.animation.core.tween

/**
 * 首页 Dashboard（深度优化版）。
 *
 * 视觉层次（自上而下）：
 *  1. 顶栏：品牌名 + 当前档案 chip
 *  2. Hero：3 环同心（蛋白 / 蔬果 / 水分） + 中央"今日进度 %"大数字
 *  3. 周/月切换 + 柱状图（达标 / 接近 / 超标 颜色编码）
 *  4. 今日余量：4 条胶囊，每条可点
 *  5. 推荐食物（Top 5）
 *
 * 设计风格：温暖圆润；走 [S] / [D] token；动效走 [Motion]。
 */
@Composable
fun HomeScreen(
    refreshKey: Int = 0,
    onViewHistory: () -> Unit = {},
    vm: HomeViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(refreshKey) { vm.refresh() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = S.xxxl),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // —— 1. 顶栏 ——
        item { HomeTopBar(state.currentProfileName) }

        // —— 2. Hero：3 环同心 ——
        item {
            Box(modifier = Modifier.padding(horizontal = S.screenH)) {
                NutritionHeroCard(state)
            }
        }

        // —— 3 + 4. 进度区 ——
        item {
            Column(
                modifier = Modifier
                    .padding(horizontal = S.screenH, vertical = S.xxl)
            ) {
                ProgressSection(state, vm, onViewHistory = onViewHistory)
            }
        }

        // —— 5. 推荐食物 Top 5 ——
        item {
            Column(
                modifier = Modifier
                    .padding(horizontal = S.screenH)
                    .padding(top = S.xxl)
            ) {
                RecommendationList(state.recommendations)
            }
        }

        item { Spacer(Modifier.height(S.huge)) }
    }
}

// ─────────────────────────────────────────────────
// 顶栏：品牌 + 档案 chip
// ─────────────────────────────────────────────────
@Composable
private fun HomeTopBar(profileName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = S.screenH, vertical = S.xl),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🌿", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.width(S.xs))
            Text(
                "LightCare",
                style = MaterialTheme.typography.headlineSmall,
                color = Primary,
                fontWeight = FontWeight.Bold
            )
        }
        if (profileName.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .background(PrimaryContainer, RoundedCornerShape(D.radiusPill))
                    .padding(horizontal = S.md, vertical = S.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("👤", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(S.xxs))
                Text(
                    profileName,
                    style = MaterialTheme.typography.labelMedium,
                    color = Primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────
// Hero：3 环同心（蛋白+蔬果+水分）+ 中央 %
// ─────────────────────────────────────────────────
@Composable
private fun NutritionHeroCard(state: HomeUiState) {
    // 3 环从外到内：蛋白 / 蔬果 / 水分
    val rings = listOf(
        RingSpec(NutrientProtein, proteinProgress(state), trackColor = NutrientProtein.copy(alpha = 0.15f)),
        RingSpec(NutrientVeg, vegProgress(state), trackColor = NutrientVeg.copy(alpha = 0.15f)),
        RingSpec(NutrientWater, waterProgress(state), trackColor = NutrientWater.copy(alpha = 0.15f))
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .ambientCard()
            .padding(S.xl),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 3 环 + 中央数据
            Box(
                modifier = Modifier.size(220.dp),
                contentAlignment = Alignment.Center
            ) {
                LCConcentricRings(
                    rings = rings,
                    size = 220.dp,
                    ringGap = 12.dp,
                    strokeWidth = 14.dp
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        greeting(state),
                        style = MaterialTheme.typography.labelMedium,
                        color = Outline
                    )
                    Spacer(Modifier.height(S.xxs))
                    Text(
                        "${overallProgress(state)}%",
                        style = MaterialTheme.typography.displayMedium,
                        color = Primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "今日完成",
                        style = MaterialTheme.typography.labelSmall,
                        color = Outline
                    )
                }
            }
            Spacer(Modifier.height(S.lg))
            // 3 环图例（小色点 + 标签 + 数值）
            Row(
                horizontalArrangement = Arrangement.spacedBy(S.md),
                modifier = Modifier.fillMaxWidth()
            ) {
                RingLegend(NutrientProtein, "蛋白", state.remnants.getOrNull(0))
                RingLegend(NutrientVeg, "蔬果", state.remnants.getOrNull(2))
                RingLegend(NutrientWater, "水分", state.remnants.getOrNull(3))
            }
        }
    }
}

@Composable
private fun RingLegend(color: Color, label: String, remnant: NutrientRemnant?) {
    val pct = remnant?.consumedPct?.toInt() ?: 0
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
            Spacer(Modifier.width(S.xxs))
            Text(label, style = MaterialTheme.typography.labelSmall, color = Outline)
        }
        Spacer(Modifier.height(S.xxs))
        Text(
            "${pct}%",
            style = MaterialTheme.typography.titleSmall,
            color = Primary,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun proteinProgress(s: HomeUiState): Float =
    s.remnants.getOrNull(0)?.consumedPct?.div(100f) ?: 0f
private fun vegProgress(s: HomeUiState): Float =
    // PRD 没列"蔬果"为余量项，从碳水近似（碳水反映主食/蔬果累计）
    s.remnants.getOrNull(2)?.consumedPct?.div(100f) ?: 0f
private fun waterProgress(s: HomeUiState): Float =
    s.remnants.getOrNull(3)?.consumedPct?.div(100f) ?: 0f
private fun overallProgress(s: HomeUiState): Int {
    val prots = listOfNotNull(
        s.remnants.getOrNull(0)?.consumedPct,
        s.remnants.getOrNull(1)?.consumedPct,
        s.remnants.getOrNull(2)?.consumedPct,
        s.remnants.getOrNull(3)?.consumedPct
    )
    if (prots.isEmpty()) return 0
    return (prots.average()).toInt()
}

private fun greeting(state: HomeUiState): String {
    val pct = overallProgress(state)
    return when {
        pct >= 100 -> "今日已达成 🌟"
        pct >= 75  -> "快到目标啦 💪"
        pct >= 40  -> "进度不错 🌱"
        state.remnants.isEmpty() -> "今天先记一餐吧"
        else       -> "慢慢来 ☕️"
    }
}

// ─────────────────────────────────────────────────
// 进度区：周/月切换 + 柱状图 + 今日余量
// ─────────────────────────────────────────────────
@Composable
private fun ProgressSection(state: HomeUiState, vm: HomeViewModel, onViewHistory: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(S.xl)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (state.view == ProgressView.WEEK) "本周进度" else "本月进度",
                style = MaterialTheme.typography.headlineMedium,
                color = Primary,
                fontWeight = FontWeight.SemiBold
            )
            ViewToggle(state.view, onChange = { vm.setView(it) })
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(state.rangeText, style = MaterialTheme.typography.labelMedium, color = Outline)
            Text(
                "查看每餐详情 ›",
                style = MaterialTheme.typography.labelMedium,
                color = Primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onViewHistory)
            )
        }
        ProgressBarChart(bars = state.bars, activeIndex = state.activeBarIndex)
        RemnantsCard(state.remnants, state.slotContribs)
    }
}

@Composable
private fun ViewToggle(view: ProgressView, onChange: (ProgressView) -> Unit) {
    Row(
        modifier = Modifier
            .background(PrimaryContainer, RoundedCornerShape(D.radiusPill))
            .padding(2.dp)
    ) {
        ProgressView.values().forEach { v ->
            val sel = v == view
            AnimatedContent(
                targetState = sel,
                transitionSpec = {
                    (fadeIn(tween(Motion.SHORT)) togetherWith fadeOut(tween(Motion.SHORT)))
                },
                label = "viewToggle"
            ) { isSel ->
                Box(
                    modifier = Modifier
                        .background(
                            if (isSel) Primary else Color.Transparent,
                            RoundedCornerShape(D.radiusPill)
                        )
                        .clickable { onChange(v) }
                        .padding(horizontal = S.lg, vertical = S.xs),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (v == ProgressView.WEEK) "周" else "月",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSel) com.lightcare.app.ui.theme.OnPrimary else Primary,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressBarChart(bars: List<DayBar>, activeIndex: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
            .padding(top = S.sm),
        horizontalArrangement = Arrangement.spacedBy(S.xs)
    ) {
        bars.forEachIndexed { i, day ->
            val isActive = i == activeIndex
            val ratio = if (day.targetKcal <= 0) 0f else day.kcal.toFloat() / day.targetKcal
            val barColor = when {
                day.kcal == 0 -> MaterialTheme.colorScheme.surfaceContainerHighest
                day.isOverTarget -> Error
                ratio > 0.9 -> Warning
                day.targetKcal > 0 -> SurgicalGreen
                else -> if (isActive) Primary else SurgicalGreen
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(
                        if (isActive) PrimaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                        RoundedCornerShape(D.radiusMd)
                    )
                    .padding(bottom = S.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                if (day.isOverTarget) {
                    Box(Modifier.size(6.dp).background(Error, CircleShape))
                    Spacer(Modifier.height(S.xxs))
                }
                val barHeight = (day.height.coerceIn(0f, 1f)) * 88f
                if (barHeight > 0.5f) {
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .height(barHeight.dp)
                            .background(barColor, RoundedCornerShape(S.sm))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(barColor, CircleShape)
                    )
                }
                Spacer(Modifier.height(S.xs))
                Text(
                    day.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isActive) Primary else Outline,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun RemnantsCard(
    remnants: List<NutrientRemnant>,
    slotContribs: List<MealSlotContribution>
) {
    // PRD：点营养条某段（某一顿）→ 弹那一顿的详情卡（蛋白/脂肪/碳水/水分 + 吃了什么）
    var selectedSlot by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .ambientCard()
            .padding(S.xl),
        verticalArrangement = Arrangement.spacedBy(S.lg)
    ) {
        com.lightcare.app.ui.theme.LCCardLabel("今日余量", emoji = "✨")
        remnants.forEach { r ->
            RemnantRow(r) { seg -> selectedSlot = seg.slot }
        }
        // 图例：今天实际吃了的餐次（按早<午<晚<加餐顺序）
        if (slotContribs.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(S.md)) {
                slotContribs.forEach { sc ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(slotColorOf(sc.slot), CircleShape)
                        )
                        Spacer(Modifier.width(S.xxs))
                        Text(sc.slotDisplay, style = MaterialTheme.typography.labelSmall, color = Outline)
                    }
                }
            }
        }
    }

    // 点某段 → 弹那一顿的详情
    selectedSlot?.let { slot ->
        slotContribs.firstOrNull { it.slot == slot }?.let { sc ->
            SlotDetailDialog(sc) { selectedSlot = null }
        }
    }
}

private fun slotColorOf(slot: String): Color = when (slot) {
    "BREAKFAST" -> SlotBreakfast
    "LUNCH"     -> SlotLunch
    "DINNER"    -> SlotDinner
    "SNACK"     -> SlotSnack
    else        -> SurgicalGreen
}

@Composable
private fun RemnantRow(r: NutrientRemnant, onSegmentClick: (NutrientSegment) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(S.md)
    ) {
        Text(
            r.name,
            style = MaterialTheme.typography.labelSmall,
            color = Primary,
            modifier = Modifier.width(40.dp),
            fontWeight = FontWeight.SemiBold
        )
        val segs = r.segments.map { seg ->
            // seg.color 来自 HomeViewModel，已按 slot 算好（每顿不同色）
            com.lightcare.app.ui.theme.SegmentSpec(Color(seg.color), seg.fraction)
        }
        LCSegmentedCapsule(
            segments = segs,
            modifier = Modifier.weight(1f),
            onSegmentClick = { idx -> r.segments.getOrNull(idx)?.let(onSegmentClick) }
        )
        Text(
            r.remain,
            style = MaterialTheme.typography.labelSmall,
            color = Primary,
            modifier = Modifier.width(78.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SlotDetailDialog(sc: MealSlotContribution, onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("关闭", color = Primary)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp).background(slotColorOf(sc.slot), CircleShape))
                Spacer(Modifier.width(S.sm))
                Text(
                    "${sc.slotDisplay} · ${sc.time}",
                    color = Primary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(S.sm)) {
                // 这顿吃了什么（来自 itemsJson；没有就用 summary 兜底）
                if (sc.foods.isNotEmpty()) {
                    Text("吃了：${sc.foods.joinToString("、") { it.name }}",
                        style = MaterialTheme.typography.bodySmall, color = Outline)
                } else {
                    Text(sc.summary, style = MaterialTheme.typography.bodySmall, color = Outline)
                }
                Spacer(Modifier.height(S.xs))
                Row(horizontalArrangement = Arrangement.spacedBy(S.sm)) {
                    com.lightcare.app.ui.theme.LCStatCell("热量", "${sc.kcal}", "kcal", modifier = Modifier.weight(1f))
                    com.lightcare.app.ui.theme.LCStatCell("蛋白", fmtG(sc.protein), "g", modifier = Modifier.weight(1f))
                    com.lightcare.app.ui.theme.LCStatCell("脂肪", fmtG(sc.fat), "g", modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(S.sm)) {
                    com.lightcare.app.ui.theme.LCStatCell("碳水", fmtG(sc.carb), "g", modifier = Modifier.weight(1f))
                    com.lightcare.app.ui.theme.LCStatCell("水分", "${sc.water}", "ml", modifier = Modifier.weight(1f))
                }
            }
        }
    )
}

private fun fmtG(v: Double): String = if (v >= 100) "${v.toInt()}" else "${"%.1f".format(v)}"

// ─────────────────────────────────────────────────
// 推荐食物
// ─────────────────────────────────────────────────
@Composable
private fun RecommendationList(recommendations: List<RecommendedFood>) {
    Column(verticalArrangement = Arrangement.spacedBy(S.md)) {
        Column {
            Text(
                "推荐食物",
                style = MaterialTheme.typography.titleLarge,
                color = Primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "按你今日的缺口排序",
                style = MaterialTheme.typography.labelSmall,
                color = Outline
            )
        }
        if (recommendations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .ambientCard()
                    .padding(S.xxl),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🌟", style = MaterialTheme.typography.displayMedium)
                    Spacer(Modifier.height(S.sm))
                    Text(
                        "今天营养已达标，无需额外推荐",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            recommendations.forEach { RecommendationItemCard(it) }
        }
    }
}

@Composable
private fun RecommendationItemCard(food: RecommendedFood) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .ambientCard()
            .clickable { /* TODO: 一键记录 */ }
            .padding(horizontal = S.lg, vertical = S.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(S.lg)
    ) {
        LCEmojiBadge(
            emoji = categoryEmojiOf(food.category),
            size = D.avatarLg,
            background = PrimaryContainer
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                food.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = Primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                food.reason,
                style = MaterialTheme.typography.bodySmall,
                color = Outline
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${food.perServingKcal}",
                style = MaterialTheme.typography.titleMedium,
                color = Primary,
                fontWeight = FontWeight.Bold
            )
            Text("kcal", style = MaterialTheme.typography.labelSmall, color = Outline)
        }
    }
}
