package com.lightcare.app.ui.food.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lightcare.app.data.api.FoodDto
import com.lightcare.app.data.api.RecipeDto
import com.lightcare.app.data.api.RecipeItemDto
import com.lightcare.app.data.api.RecipeStepDto
import com.lightcare.app.ui.theme.Background
import com.lightcare.app.ui.theme.BorderSubtle
import com.lightcare.app.ui.theme.D
import com.lightcare.app.ui.theme.LCEmptyState
import com.lightcare.app.ui.theme.LCLoading
import com.lightcare.app.ui.theme.LCTopBar
import com.lightcare.app.ui.theme.NutrientKcal
import com.lightcare.app.ui.theme.NutrientProtein
import com.lightcare.app.ui.theme.NutrientVeg
import com.lightcare.app.ui.theme.Outline
import com.lightcare.app.ui.theme.Primary
import com.lightcare.app.ui.theme.PrimaryContainer
import com.lightcare.app.ui.theme.OnPrimary
import com.lightcare.app.ui.theme.S
import com.lightcare.app.ui.theme.Surface
import com.lightcare.app.ui.theme.SurfaceContainer
import com.lightcare.app.ui.theme.SurfaceVariant
import com.lightcare.app.ui.theme.Warning
import com.lightcare.app.ui.theme.ambientCard
import com.lightcare.app.ui.theme.categoryEmojiOf

/**
 * 食物详情（PR-Recipe）。
 *
 * 展示顺序：基础营养（克数 / kcal / 蛋白 / 脂肪 / 碳水）→ 烹饪信息 → 食材 → 调料 → 步骤。
 * 无做法时给空状态 CTA（"添加做法"），点击打开 [RecipeEditSheet]。
 */
@Composable
fun FoodDetailScreen(
    onBack: () -> Unit,
    vm: FoodDetailViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showEditor by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.load() }

    // 保存提示条：3 秒后自动消失。消息清空由 VM 处理。
    LaunchedEffect(state.saveMessage) {
        if (state.saveMessage != null) {
            kotlinx.coroutines.delay(3000)
            vm.clearSaveMessage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        LCTopBar(
            title = "食物详情",
            onBack = onBack,
            actions = {
                if (state.food?.isDefault == false) {
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .clickable { showEditor = true }
                            .padding(horizontal = S.md),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "编辑做法",
                            style = MaterialTheme.typography.labelLarge,
                            color = Primary,
                            fontWeight = FontWeight.SemiBold
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
            state.food == null -> LCEmptyState(
                emoji = "🍽️",
                message = "找不到该食物",
                actionLabel = "返回",
                onAction = onBack
            )
            else -> FoodDetailContent(
                food = state.food!!,
                recipe = state.recipe,
                onAddRecipe = { showEditor = true }
            )
        }

        // 一次性提示条（已保存 / 错误）
        state.saveMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceVariant)
                    .padding(horizontal = S.lg, vertical = S.sm)
            ) {
                Text(msg, style = MaterialTheme.typography.bodySmall, color = Outline)
            }
        }
    }

    if (showEditor && state.food != null) {
        RecipeEditSheet(
            initial = state.recipe ?: RecipeDto(foodId = state.food!!.id),
            saving = state.saving,
            onDismiss = { showEditor = false },
            onSave = { req ->
                vm.upsertRecipe(req)
                showEditor = false
            }
        )
    }
}

@Composable
private fun FoodDetailContent(
    food: FoodDto,
    recipe: RecipeDto?,
    onAddRecipe: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = S.xxxl)
    ) {
        // === 食物基础信息 ===
        item("header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = S.lg, vertical = S.lg)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        categoryEmojiOf(food.category),
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Spacer(Modifier.width(S.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            food.displayName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "${food.category} · ${food.perServingG}g / 份",
                            style = MaterialTheme.typography.bodySmall,
                            color = Outline
                        )
                    }
                }
            }
        }

        // === 营养卡 ===
        item("nutrition") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = S.lg)
                    .ambientCard()
                    .padding(S.lg)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(S.md)) {
                    Text(
                        "营养（每份）",
                        style = MaterialTheme.typography.labelLarge,
                        color = Outline,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        NutrientCell("热量", "${food.kcal}", "kcal", NutrientKcal)
                        NutrientCell("蛋白", formatG(food.proteinG), "g", NutrientProtein)
                        NutrientCell("脂肪", formatG(food.fatG), "g", Warning)
                        NutrientCell("碳水", formatG(food.carbG), "g", NutrientVeg)
                    }
                    if (food.fiberG > 0 || food.waterMl > 0 || food.vegServings > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (food.fiberG > 0) NutrientCell("纤维", formatG(food.fiberG), "g", Primary)
                            if (food.waterMl > 0) NutrientCell("含水", "${food.waterMl}", "ml", Primary)
                            if (food.vegServings > 0) NutrientCell("蔬果份", "${food.vegServings}", "", Primary)
                        }
                    }
                }
            }
        }

        item("spacer1") { Spacer(Modifier.height(S.lg)) }

        // === 烹饪信息（时间 / 难度） ===
        if (recipe != null && !recipe.isEmpty) {
            item("cook") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = S.lg)
                        .ambientCard()
                        .padding(S.lg)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "烹饪时间",
                                style = MaterialTheme.typography.labelLarge,
                                color = Outline,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "${recipe.cookingMinutes}",
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontFeatureSettings = "tnum"
                                ),
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                            Text(
                                "分钟",
                                style = MaterialTheme.typography.bodySmall,
                                color = Outline
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "难度",
                                style = MaterialTheme.typography.labelLarge,
                                color = Outline,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(S.xs))
                            DifficultyChip(recipe.difficulty)
                        }
                    }
                }
            }
            item("spacer2") { Spacer(Modifier.height(S.lg)) }
        }

        // === 食材清单 ===
        if (recipe != null && recipe.ingredients.isNotEmpty()) {
            item("ingredients") {
                RecipeListSection(
                    title = "食材",
                    emoji = "🥬",
                    rows = recipe.ingredients.map { "${it.name}${if (it.amount.isNotBlank()) "  ${it.amount}" else ""}" }
                )
            }
            item("spacer3") { Spacer(Modifier.height(S.md)) }
        }

        // === 调料清单 ===
        if (recipe != null && recipe.seasonings.isNotEmpty()) {
            item("seasonings") {
                RecipeListSection(
                    title = "调料",
                    emoji = "🧂",
                    rows = recipe.seasonings.map { "${it.name}${if (it.amount.isNotBlank()) "  ${it.amount}" else ""}" }
                )
            }
            item("spacer4") { Spacer(Modifier.height(S.md)) }
        }

        // === 步骤 ===
        if (recipe != null && recipe.steps.isNotEmpty()) {
            item("steps") { RecipeStepsSection(steps = recipe.steps) }
            item("spacer5") { Spacer(Modifier.height(S.lg)) }
        }

        // === 空状态 CTA（无做法）===
        if (recipe == null || recipe.isEmpty) {
            item("empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = S.lg)
                        .ambientCard()
                        .padding(S.lg)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(S.sm)
                    ) {
                        Text(
                            "🍳",
                            style = MaterialTheme.typography.displayMedium
                        )
                        Text(
                            "还没有做法",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            if (food.isDefault) "内置食物暂不支持编辑做法" else "拍菜 / 手填都支持，给这道菜记个做法吧",
                            style = MaterialTheme.typography.bodySmall,
                            color = Outline
                        )
                        if (!food.isDefault) {
                            Spacer(Modifier.height(S.sm))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp)
                                    .background(Primary, RoundedCornerShape(D.radiusPill))
                                    .clickable { onAddRecipe() }
                                    .padding(horizontal = S.lg, vertical = S.sm),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "添加做法",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OnPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NutrientCell(
    label: String,
    value: String,
    unit: String,
    accent: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Outline,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            value,
            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.Bold,
            color = accent
        )
        if (unit.isNotBlank()) {
            Text(
                unit,
                style = MaterialTheme.typography.labelSmall,
                color = Outline
            )
        }
    }
}

@Composable
private fun DifficultyChip(difficulty: String) {
    val (text, bg, fg) = when (difficulty.uppercase()) {
        "HARD" -> Triple("难", SurfaceContainer, Outline)
        "MEDIUM" -> Triple("中", PrimaryContainer, Primary)
        else -> Triple("易", Primary, OnPrimary)
    }
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(D.radiusPill))
            .padding(horizontal = S.md, vertical = S.xs)
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = fg, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RecipeListSection(title: String, emoji: String, rows: List<String>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = S.lg)
            .ambientCard()
            .padding(S.lg)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(S.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(S.sm))
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            rows.forEach { row ->
                Text(
                    row,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun RecipeStepsSection(steps: List<RecipeStepDto>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = S.lg)
            .ambientCard()
            .padding(S.lg)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(S.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📝", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(S.sm))
                Text(
                    "步骤",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            steps.sortedBy { it.order }.forEachIndexed { idx, step ->
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        "${idx + 1}.",
                        style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                    Spacer(Modifier.width(S.sm))
                    Text(
                        step.text.ifBlank { "（未填写）" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/** "12.0" → "12"，"12.5" → "12.5"。少小数尾巴。 */
private fun formatG(v: Double): String {
    if (v <= 0) return "0"
    val rounded = (v * 10).toLong() / 10.0
    return if (rounded == rounded.toLong().toDouble()) "${rounded.toLong()}"
           else "%.1f".format(rounded)
}