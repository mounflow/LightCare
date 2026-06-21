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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lightcare.app.data.api.RecipeDto
import com.lightcare.app.data.api.RecipeItemDto
import com.lightcare.app.data.api.RecipeStepDto
import com.lightcare.app.data.api.UpsertRecipeReq
import com.lightcare.app.ui.theme.D
import com.lightcare.app.ui.theme.Error
import com.lightcare.app.ui.theme.OnPrimary
import com.lightcare.app.ui.theme.OnSurface
import com.lightcare.app.ui.theme.Outline
import com.lightcare.app.ui.theme.Primary
import com.lightcare.app.ui.theme.PrimaryContainer
import com.lightcare.app.ui.theme.S
import com.lightcare.app.ui.theme.Surface
import com.lightcare.app.ui.theme.SurfaceVariant
import com.lightcare.app.ui.theme.BorderSubtle

/**
 * 做法编辑底部弹层（PR-Recipe）。
 *
 * 三段可增删行：食材 / 调料 / 步骤。
 * - 上半屏：烹饪时间（数字输入）+ 难度三选一 chip。
 * - 下半屏：LazyColumn 三个可加行的段；每行右侧 48dp "✕" 删行，"＋" 在段尾新增空行。
 * - 底部固定 [取消] / [保存] 主操作条。
 *
 * 保存：把所有非空字段塞进 [UpsertRecipeReq]，传 VM（FoodDetailViewModel.upsertRecipe）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditSheet(
    initial: RecipeDto,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (UpsertRecipeReq) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var minutesText by remember { mutableStateOf(initial.cookingMinutes.takeIf { it > 0 }?.toString() ?: "") }
    var difficulty by remember { mutableStateOf(initial.difficulty.ifBlank { "EASY" }) }

    val ingredients = remember { mutableStateListOf<Row>().apply { addAll(initial.ingredients.map { Row(it.name, it.amount) }) } }
    val seasonings  = remember { mutableStateListOf<Row>().apply { addAll(initial.seasonings.map {  Row(it.name, it.amount) }) } }
    val steps       = remember { mutableStateListOf<String>().apply { addAll(initial.steps.sortedBy { it.order }.map { it.text }) } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface)
        ) {
            // 标题条
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = S.lg, vertical = S.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "编辑做法",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurface,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onDismiss) {
                    Text("关闭", color = Outline, style = MaterialTheme.typography.labelLarge)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = S.lg, vertical = S.sm)
            ) {
                // === 烹饪时间 + 难度 ===
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(D.hairline, BorderSubtle, RoundedCornerShape(D.radiusMd))
                            .padding(S.md)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(S.sm)) {
                            Text(
                                "烹饪信息",
                                style = MaterialTheme.typography.labelLarge,
                                color = Outline,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(S.sm),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = minutesText,
                                    onValueChange = { v ->
                                        minutesText = v.filter { it.isDigit() }.take(4)
                                    },
                                    label = { Text("时间（分钟）") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(S.sm)) {
                                listOf("EASY" to "易", "MEDIUM" to "中", "HARD" to "难").forEach { (k, label) ->
                                    DifficultyPick(label = label, selected = difficulty == k, onClick = { difficulty = k })
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(S.md))
                }

                // === 食材 ===
                item {
                    ListEditor(
                        title = "食材",
                        emoji = "🥬",
                        rows = ingredients,
                        addHint = "例如 西红柿",
                        amountHint = "2 个"
                    )
                    Spacer(Modifier.height(S.md))
                }

                // === 调料 ===
                item {
                    ListEditor(
                        title = "调料",
                        emoji = "🧂",
                        rows = seasonings,
                        addHint = "例如 盐",
                        amountHint = "少许"
                    )
                    Spacer(Modifier.height(S.md))
                }

                // === 步骤 ===
                item {
                    StepsEditor(
                        steps = steps,
                        addHint = "例如 西红柿切块"
                    )
                    Spacer(Modifier.height(S.lg))
                }
            }

            // === 底部主操作条 ===
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceVariant)
                    .padding(horizontal = S.lg, vertical = S.md),
                horizontalArrangement = Arrangement.spacedBy(S.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .background(Surface, RoundedCornerShape(D.radiusPill))
                        .border(D.hairline, BorderSubtle, RoundedCornerShape(D.radiusPill))
                        .clickable { onDismiss() }
                        .padding(vertical = S.sm),
                    contentAlignment = Alignment.Center
                ) {
                    Text("取消", style = MaterialTheme.typography.labelLarge, color = Outline, fontWeight = FontWeight.SemiBold)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .background(Primary, RoundedCornerShape(D.radiusPill))
                        .clickable(enabled = !saving) {
                            onSave(buildReq(minutesText, difficulty, ingredients, seasonings, steps))
                        }
                        .padding(vertical = S.sm),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (saving) "保存中…" else "保存",
                        style = MaterialTheme.typography.labelLarge,
                        color = OnPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private fun buildReq(
    minutesText: String,
    difficulty: String,
    ingredients: SnapshotStateList<Row>,
    seasonings: SnapshotStateList<Row>,
    steps: SnapshotStateList<String>
): UpsertRecipeReq {
    val mins = minutesText.toIntOrNull() ?: 0
    return UpsertRecipeReq(
        cookingMinutes = mins,
        difficulty = difficulty,
        ingredients = ingredients.filter { it.name.isNotBlank() }.map { RecipeItemDto(it.name.trim(), it.amount.trim()) }
            .ifEmpty { null /* 空数组 = 不修改已存在的 */ },
        seasonings = seasonings.filter { it.name.isNotBlank() }.map { RecipeItemDto(it.name.trim(), it.amount.trim()) }
            .ifEmpty { null },
        steps = steps.filter { it.isNotBlank() }.mapIndexed { i, t -> RecipeStepDto(i + 1, t.trim()) }
            .ifEmpty { null },
        source = "MANUAL"
    )
}

@Composable
private fun DifficultyPick(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) Primary else PrimaryContainer
    val fg = if (selected) OnPrimary else Primary
    Box(
        modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .background(bg, RoundedCornerShape(D.radiusPill))
            .clickable(onClick = onClick)
            .padding(horizontal = S.lg, vertical = S.xs),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = fg, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ListEditor(
    title: String,
    emoji: String,
    rows: SnapshotStateList<Row>,
    addHint: String,
    amountHint: String
) {
    SectionFrame(title = title, emoji = emoji) {
        Column(verticalArrangement = Arrangement.spacedBy(S.xs)) {
            rows.forEachIndexed { idx, r ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(S.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = r.name,
                        onValueChange = { v -> rows[idx] = r.copy(name = v.take(20)) },
                        placeholder = { Text(addHint, style = MaterialTheme.typography.bodySmall) },
                        singleLine = true,
                        modifier = Modifier.weight(1.6f)
                    )
                    OutlinedTextField(
                        value = r.amount,
                        onValueChange = { v -> rows[idx] = r.copy(amount = v.take(12)) },
                        placeholder = { Text(amountHint, style = MaterialTheme.typography.bodySmall) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    DeleteBtn(onClick = { rows.removeAt(idx) })
                }
            }
            AddRowBtn(text = "添加${title}", onClick = { rows.add(Row("", "")) })
        }
    }
}

@Composable
private fun StepsEditor(steps: SnapshotStateList<String>, addHint: String) {
    SectionFrame(title = "步骤", emoji = "📝") {
        Column(verticalArrangement = Arrangement.spacedBy(S.xs)) {
            steps.forEachIndexed { idx, text ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(S.xs),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        "${idx + 1}.",
                        style = MaterialTheme.typography.titleMedium,
                        color = Primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 14.dp)
                    )
                    OutlinedTextField(
                        value = text,
                        onValueChange = { v -> steps[idx] = v.take(200) },
                        placeholder = { Text(addHint, style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.weight(1f),
                        maxLines = 4
                    )
                    DeleteBtn(onClick = { steps.removeAt(idx) })
                }
            }
            AddRowBtn(text = "添加步骤", onClick = { steps.add("") })
        }
    }
}

@Composable
private fun SectionFrame(title: String, emoji: String, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(D.hairline, BorderSubtle, RoundedCornerShape(D.radiusMd))
            .padding(S.md)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(S.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(S.sm))
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Outline
                )
            }
            content()
        }
    }
}

@Composable
private fun DeleteBtn(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text("✕", style = MaterialTheme.typography.titleLarge, color = Error, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AddRowBtn(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .border(D.hairline, BorderSubtle, RoundedCornerShape(D.radiusPill))
            .clickable(onClick = onClick)
            .padding(vertical = S.xs),
        contentAlignment = Alignment.Center
    ) {
        Text("＋ $text", color = Primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

/** 食材 / 调料的"名 + 用量"一行（UI 状态用，不是 DTO）。 */
private data class Row(val name: String, val amount: String)