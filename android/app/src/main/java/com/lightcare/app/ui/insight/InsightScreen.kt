package com.lightcare.app.ui.insight

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lightcare.app.data.api.HighlightDto
import com.lightcare.app.data.api.NutritionDto
import com.lightcare.app.data.api.WeeklyReportDto
import com.lightcare.app.ui.theme.D
import com.lightcare.app.ui.theme.LCEmptyState
import com.lightcare.app.ui.theme.LCLoading
import com.lightcare.app.ui.theme.LCStatCell
import com.lightcare.app.ui.theme.LCTopBar
import com.lightcare.app.ui.theme.NutrientKcal
import com.lightcare.app.ui.theme.NutrientProtein
import com.lightcare.app.ui.theme.NutrientVeg
import com.lightcare.app.ui.theme.NutrientWater
import com.lightcare.app.ui.theme.OnPrimary
import com.lightcare.app.ui.theme.OnSecondaryContainer
import com.lightcare.app.ui.theme.Outline
import com.lightcare.app.ui.theme.Primary
import com.lightcare.app.ui.theme.PrimaryContainer
import com.lightcare.app.ui.theme.S
import com.lightcare.app.ui.theme.SecondaryContainer
import com.lightcare.app.ui.theme.ambientCard
import com.lightcare.app.ui.theme.surgicalAccent

/**
 * 周报与复盘（深度优化版）。
 *
 * 视觉：
 * - 顶栏带分享图标
 * - AI 点评卡：柔绿左胶囊 + sparkles 图标
 * - 4 环（能量/蛋白/蔬果/水分） 单环 + 中心大数字
 * - 本周概览：横向 2 个统计 cell
 * - 亮点：emoji + 标题 + 正文
 */
@Composable
fun InsightScreen(
    onViewHistory: () -> Unit = {},
    onShare: () -> Unit = {},
    vm: InsightViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LCTopBar(
            title = "数据",
            emoji = "📊",
            actions = {
                if (state.report != null) {
                    Box(
                        modifier = Modifier
                            .size(D.topBar - 16.dp)
                            .clickable(onClick = onShare),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Share,
                            contentDescription = "分享周报",
                            tint = Primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        )

        when {
            state.loading -> LCLoading()
            state.error != null -> LCEmptyState(
                emoji = "😅",
                message = state.error!!,
                actionLabel = "重试",
                onAction = { vm.load() }
            )
            state.report != null -> {
                val report = state.report!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = S.xxxl),
                    verticalArrangement = Arrangement.spacedBy(S.lg)
                ) {
                    item { Box(Modifier.padding(horizontal = S.screenH)) { AiReviewCard(report.praise) } }
                    item { Box(Modifier.padding(horizontal = S.screenH)) { NutritionRing(report.nutrition) } }
                    item {
                        Box(Modifier.padding(horizontal = S.screenH)) {
                            OverviewRow(
                                daysLogged = report.daysLogged,
                                mealCount = report.mealCount,
                                weekStart = report.weekStart,
                                weekEnd = report.weekEnd,
                                onViewHistory = onViewHistory
                            )
                        }
                    }
                    if (report.highlights.isNotEmpty()) {
                        item {
                            Text(
                                "本周亮点",
                                style = MaterialTheme.typography.titleLarge,
                                color = Primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = S.screenH, top = S.md, bottom = S.xs)
                            )
                        }
                        items(report.highlights) { h ->
                            Box(Modifier.padding(horizontal = S.screenH)) { HighlightCard(h) }
                        }
                    }
                }
            }
            else -> LCEmptyState(emoji = "📋", message = "暂无周报数据")
        }
    }
}

@Composable
private fun AiReviewCard(praise: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .ambientCard()
            .surgicalAccent()
            .padding(S.xl),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(D.avatarMd)
                .background(SecondaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(S.lg))
        Column(verticalArrangement = Arrangement.spacedBy(S.xs)) {
            com.lightcare.app.ui.theme.LCCardLabel("AI 点评", emoji = "🥗")
            Text(
                praise.ifBlank { "继续保持，本周表现不错～" },
                style = MaterialTheme.typography.bodyMedium,
                color = Primary
            )
        }
    }
}

@Composable
private fun NutritionRing(n: NutritionDto) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .ambientCard()
            .padding(S.xl),
        verticalArrangement = Arrangement.spacedBy(S.md)
    ) {
        com.lightcare.app.ui.theme.LCCardLabel("本周达标率", emoji = "🎯")
        // 2×2 进度条卡片网格（替代原圆环，更清爽、信息密度高）
        Row(horizontalArrangement = Arrangement.spacedBy(S.md)) {
            ProgressStat("🔥", "能量", n.kcalPct, NutrientKcal, Modifier.weight(1f))
            ProgressStat("💪", "蛋白", n.proteinPct, NutrientProtein, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(S.md)) {
            ProgressStat("🥦", "蔬果", n.vegPct, NutrientVeg, Modifier.weight(1f))
            ProgressStat("💧", "水分", n.waterPct, NutrientWater, Modifier.weight(1f))
        }
    }
}

/** 达标率卡片：emoji+标签 + 大百分比 + 横向进度条（进度条用该营养项的专属色）。 */
@Composable
private fun ProgressStat(
    emoji: String, label: String, pct: Int,
    color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier
) {
    val safe = pct.coerceIn(0, 100)
    Column(
        modifier = modifier
            .background(color.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
            .padding(S.md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(S.xs))
            Text(label, style = MaterialTheme.typography.labelMedium, color = Outline)
            Spacer(Modifier.weight(1f))
            Text("${safe}%", style = MaterialTheme.typography.titleLarge,
                color = Primary, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(S.sm))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(50))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((safe / 100f))
                    .background(color, RoundedCornerShape(50))
            )
        }
    }
}

@Composable
private fun OverviewRow(
    daysLogged: Int,
    mealCount: Int,
    weekStart: String,
    weekEnd: String,
    onViewHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .ambientCard()
            .padding(S.xl),
        verticalArrangement = Arrangement.spacedBy(S.md)
    ) {
        com.lightcare.app.ui.theme.LCCardLabel("本周概览", emoji = "📅")
        Row(horizontalArrangement = Arrangement.spacedBy(S.md), modifier = Modifier.fillMaxWidth()) {
            LCStatCell("记录天数", "$daysLogged", "天", emoji = "📝", modifier = Modifier.weight(1f))
            LCStatCell("餐次", "$mealCount", "餐", emoji = "🍱", modifier = Modifier.weight(1f))
        }
        Text(
            "$weekStart ~ $weekEnd",
            style = MaterialTheme.typography.labelMedium,
            color = Outline
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onViewHistory),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                "查看每餐详情 ›",
                style = MaterialTheme.typography.labelLarge,
                color = Primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun HighlightCard(h: HighlightDto) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .ambientCard()
            .padding(S.lg),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(D.avatarMd)
                .background(PrimaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                h.emoji.ifBlank { "•" },
                style = MaterialTheme.typography.titleLarge
            )
        }
        Spacer(Modifier.width(S.md))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                h.title,
                style = MaterialTheme.typography.titleMedium,
                color = Primary,
                fontWeight = FontWeight.SemiBold
            )
            if (h.body.isNotBlank()) {
                Text(
                    h.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSecondaryContainer
                )
            }
        }
    }
}
