package com.lightcare.app.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lightcare.app.data.db.MealCacheEntity
import com.lightcare.app.ui.theme.D
import com.lightcare.app.ui.theme.LCEmptyState
import com.lightcare.app.ui.theme.LCLoading
import com.lightcare.app.ui.theme.LCTopBar
import com.lightcare.app.ui.theme.NutrientKcal
import com.lightcare.app.ui.theme.Outline
import com.lightcare.app.ui.theme.Primary
import com.lightcare.app.ui.theme.PrimaryContainer
import com.lightcare.app.ui.theme.OnPrimary
import com.lightcare.app.ui.theme.S
import com.lightcare.app.ui.theme.Surface
import com.lightcare.app.ui.theme.ZoomScrim
import com.lightcare.app.ui.theme.ambientCard

/**
 * 每餐记录信息流页（深度优化版）。
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun MealHistoryScreen(
    onBack: () -> Unit,
    vm: MealHistoryViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var zoomBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LCTopBar(
            title = "本周记录",
            onBack = onBack,
            actions = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .clickable { vm.prevWeek() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("‹", style = MaterialTheme.typography.titleLarge, color = Primary)
                    }
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .clickable(enabled = state.canGoNext) { vm.nextWeek() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "›",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (state.canGoNext) Primary else Outline
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
            state.sections.isEmpty() -> LCEmptyState(
                emoji = "🍽️",
                message = "本周还没有记录，去记一餐吧",
                actionLabel = "刷新看看",
                onAction = { vm.load() }
            )
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = S.xxxl)
                ) {
                    state.sections.forEach { section ->
                        item(key = "header_${section.dateLabel}") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(PrimaryContainer.copy(alpha = 0.4f))
                                    .padding(horizontal = S.lg, vertical = S.md)
                            ) {
                                Text(
                                    section.dateLabel,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        items(section.meals, key = { it.id }) { meal ->
                            Box(modifier = Modifier.padding(horizontal = S.lg, vertical = S.md)) {
                                MealCard(
                                    meal = meal,
                                    loader = vm.imageLoader,
                                    onImageClick = { zoomBitmap = it },
                                    onDelete = { vm.deleteMeal(meal.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    zoomBitmap?.let { bmp ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ZoomScrim)
                .clickable { zoomBitmap = null },
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "放大图",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun MealCard(
    meal: MealCacheEntity,
    loader: MealImageLoader,
    onImageClick: (android.graphics.Bitmap?) -> Unit,
    onDelete: () -> Unit
) {
    val (slotName, slotEmoji) = slotDisplay(meal.slot)
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .ambientCard()
            .combinedClickable(
                onClick = {},
                onLongClick = { showDeleteDialog = true }
            )
            .padding(S.xl),
        verticalArrangement = Arrangement.spacedBy(S.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$slotEmoji $slotName",
                    style = MaterialTheme.typography.titleSmall,
                    color = Primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(S.sm))
                Text(meal.mealTime.take(5), style = MaterialTheme.typography.labelMedium, color = Outline)
            }
            if (meal.location.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📍", style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.width(2.dp))
                    Text(
                        meal.location,
                        style = MaterialTheme.typography.labelSmall,
                        color = Outline,
                        maxLines = 1
                    )
                }
            }
        }
        if (meal.description.isNotBlank()) {
            Text(
                meal.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Outline,
                modifier = Modifier.fillMaxWidth()
            )
        } else if (meal.summary.isNotBlank()) {
            Text(
                meal.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = Outline,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (meal.source == "PHOTO") {
            MealBigImage(mealId = meal.id, loader = loader, onClick = onImageClick)
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(S.sm),
            verticalArrangement = Arrangement.spacedBy(S.sm)
        ) {
            NutrientChip("热量", "${meal.kcal}", "kcal", emphasize = true, color = NutrientKcal)
            NutrientChip("蛋白", fmt(meal.proteinG), "g")
            NutrientChip("脂肪", fmt(meal.fatG), "g")
            NutrientChip("碳水", fmt(meal.carbG), "g")
            if (meal.fiberG > 0) NutrientChip("纤维", fmt(meal.fiberG), "g")
            if (meal.waterMl > 0) NutrientChip("水分", "${meal.waterMl}", "ml")
            if (meal.vegServings > 0) NutrientChip("蔬果", "${meal.vegServings}", "份")
        }
    }

    if (showDeleteDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除这条记录？", color = Primary, fontWeight = FontWeight.Bold) },
            text = { Text("$slotName · ${meal.summary}", style = MaterialTheme.typography.bodySmall, color = Outline) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) { Text("删除", color = com.lightcare.app.ui.theme.Error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消", color = Outline)
                }
            }
        )
    }
}

@Composable
private fun MealBigImage(
    mealId: Long,
    loader: MealImageLoader,
    onClick: (android.graphics.Bitmap?) -> Unit
) {
    val bmp by produceState<android.graphics.Bitmap?>(initialValue = null, mealId) {
        value = loader.load(mealId)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(Surface, RoundedCornerShape(D.radiusMd))
            .clickable { if (bmp != null) onClick(bmp) },
        contentAlignment = Alignment.Center
    ) {
        if (bmp != null) {
            androidx.compose.foundation.Image(
                bitmap = bmp!!.asImageBitmap(),
                contentDescription = "美食大图",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            androidx.compose.material3.CircularProgressIndicator(
                color = Primary,
                strokeWidth = D.thick,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun NutrientChip(
    label: String, value: String, unit: String,
    emphasize: Boolean = false,
    color: androidx.compose.ui.graphics.Color = Primary
) {
    val bg = if (emphasize) color else PrimaryContainer
    val fg = if (emphasize) OnPrimary else com.lightcare.app.ui.theme.OnPrimaryContainer
    Column(
        modifier = Modifier
            .background(bg, RoundedCornerShape(D.radiusSm))
            .padding(horizontal = S.md, vertical = S.xs),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = 0.85f))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = MaterialTheme.typography.titleMedium, color = fg, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(2.dp))
            Text(unit, style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = 0.7f))
        }
    }
}

private fun fmt(v: Double): String = if (v >= 100) "${v.toInt()}" else "%.1f".format(v)
