package com.lightcare.app.ui.food

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.withTimeoutOrNull
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lightcare.app.data.food.FoodItem
import com.lightcare.app.ui.theme.BorderSubtle
import com.lightcare.app.ui.theme.D
import com.lightcare.app.ui.theme.Error
import com.lightcare.app.ui.theme.LCEmojiBadge
import com.lightcare.app.ui.theme.LCEmptyState
import com.lightcare.app.ui.theme.LCPrimaryButton
import com.lightcare.app.ui.theme.LCTopBar
import com.lightcare.app.ui.theme.OnPrimary
import com.lightcare.app.ui.theme.Outline
import com.lightcare.app.ui.theme.Primary
import com.lightcare.app.ui.theme.PrimaryContainer
import com.lightcare.app.ui.theme.S
import com.lightcare.app.ui.theme.categoryEmojiOf

/**
 * 食物库管理页（深度优化版）。
 */
@Composable
fun FoodLibraryScreen(
    onBack: () -> Unit,
    vm: FoodLibraryViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<FoodItem?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var builtinHint by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (state.selectionMode) {
            SelectionToolbar(
                count = state.selectedCustomIds.size,
                onCancel = { vm.exitSelection() },
                onSelectAll = { vm.selectAll() },
                onDelete = { vm.deleteSelected() }
            )
        } else {
            LCTopBar(
                title = "我的食物库",
                onBack = onBack,
                actions = {
                    if (state.customs.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(D.topBar - 16.dp)
                                .clickable(onClick = { showClearConfirm = true }),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.DeleteSweep,
                                contentDescription = "清空自定义",
                                tint = Error
                            )
                        }
                    }
                }
            )
        }

        var query by remember { mutableStateOf("") }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("搜索食物，如：鸡胸、米饭", style = MaterialTheme.typography.labelMedium) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = Outline) },
            singleLine = true,
            shape = RoundedCornerShape(D.radiusMd),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = S.screenH, vertical = S.sm)
        )
        val q = query.trim()
        val customsFiltered = if (q.isEmpty()) state.customs else state.customs.filter {
            it.displayName.contains(q, ignoreCase = true) || it.category.contains(q, ignoreCase = true)
        }
        val defaultsFiltered = if (q.isEmpty()) state.defaults else state.defaults.filter {
            it.displayName.contains(q, ignoreCase = true) || it.category.contains(q, ignoreCase = true)
        }

        if (customsFiltered.isEmpty() && defaultsFiltered.isEmpty() && q.isNotEmpty()) {
            LCEmptyState(
                emoji = "🔍",
                message = "没找到「$q」，点底部 + 添加",
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = S.screenH),
                verticalArrangement = Arrangement.spacedBy(S.md),
                contentPadding = PaddingValues(bottom = S.huge)
            ) {
                if (customsFiltered.isNotEmpty()) {
                    item { SectionHeader("自定义（可长按多选）") }
                    items(customsFiltered, key = { it.key }) { item ->
                        FoodRow(
                            item = item,
                            selectionMode = state.selectionMode,
                            selected = item.customId in state.selectedCustomIds,
                            onClick = {
                                if (state.selectionMode) vm.toggleSelect(item)
                                else editing = item
                            },
                            onLongClick = {
                                if (!state.selectionMode) vm.enterSelection(item)
                                else vm.toggleSelect(item)
                            }
                        )
                    }
                }
                if (defaultsFiltered.isNotEmpty()) {
                    item { SectionHeader("内置 ${defaultsFiltered.size} 条（只读，长按多选删除）") }
                    items(defaultsFiltered, key = { it.key }) { item ->
                        FoodRow(
                            item = item,
                            selectionMode = state.selectionMode,
                            selected = item.customId in state.selectedCustomIds,
                            onClick = {
                                if (state.selectionMode) vm.toggleSelect(item)
                                else builtinHint = item.displayName
                            },
                            onLongClick = {
                                if (!state.selectionMode) vm.enterSelection(item)
                                else vm.toggleSelect(item)
                            }
                        )
                    }
                }
            }
        }

        if (!state.selectionMode) {
            Box(modifier = Modifier.padding(S.screenH)) {
                LCPrimaryButton(
                    text = "添加食物",
                    onClick = { showAdd = true },
                    leadingEmoji = "＋"
                )
            }
        }
    }

    if (showAdd) {
        AddFoodDialog(
            onDismiss = { showAdd = false },
            onConfirm = { input ->
                vm.add(input)
                showAdd = false
            },
            title = "添加食物"
        )
    }
    editing?.let { item ->
        EditFoodDialog(
            item = item,
            onDismiss = { editing = null },
            onSave = { input ->
                if (item.isDefault) vm.overrideDefault(item, input)
                else {
                    val id = item.customId ?: return@EditFoodDialog
                    vm.update(id, input)
                }
                editing = null
            }
        )
    }
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("清空所有自定义食物？", fontWeight = FontWeight.Bold) },
            text = { Text("将删除你添加的所有自定义食物（内置 22 条不受影响）。此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearAllCustom()
                    showClearConfirm = false
                }) { Text("清空", color = Error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showClearConfirm = false }) { Text("取消") } }
        )
    }
    builtinHint?.let { name ->
        AlertDialog(
            onDismissRequest = { builtinHint = null },
            title = { Text("「$name」是内置食物", fontWeight = FontWeight.Bold) },
            text = { Text("内置食物暂不支持直接编辑。如需调整营养，请点底部「+」添加你自己的版本。") },
            confirmButton = {
                TextButton(onClick = {
                    builtinHint = null
                    showAdd = true
                }) { Text("去添加", color = Primary, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { builtinHint = null }) { Text("知道了") } }
        )
    }
    state.message?.let { msg ->
        AlertDialog(
            onDismissRequest = vm::clearMessage,
            confirmButton = { TextButton(onClick = vm::clearMessage) { Text("好") } },
            text = { Text(msg) }
        )
    }
}

@Composable
private fun SelectionToolbar(count: Int, onCancel: () -> Unit, onSelectAll: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PrimaryContainer)
            .padding(horizontal = S.sm, vertical = S.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(D.topBar - 16.dp)
                .clickable(onClick = onCancel),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Close, contentDescription = "取消多选", tint = Primary)
        }
        Text(
            "已选 $count 项",
            style = MaterialTheme.typography.titleMedium,
            color = Primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = S.sm),
            textAlign = TextAlign.Center
        )
        Box(
            modifier = Modifier
                .size(D.topBar - 16.dp)
                .clickable(onClick = onSelectAll),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.DoneAll, contentDescription = "全选", tint = Primary)
        }
        Box(
            modifier = Modifier
                .size(D.topBar - 16.dp)
                .background(if (count > 0) Error else Outline, CircleShape)
                .clickable(enabled = count > 0, onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Delete, contentDescription = "删除", tint = OnPrimary)
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = Outline,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = S.sm, bottom = S.xxs)
    )
}

@Composable
private fun FoodRow(
    item: FoodItem,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selectionMode && selected) PrimaryContainer
                else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(D.radiusMd)
            )
            .border(D.hairline, BorderSubtle, RoundedCornerShape(D.radiusMd))
            .clickable(onClick = onClick)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitFirstDown(requireUnconsumed = false)
                        val upOrCancel = withTimeoutOrNull(500L) { waitForUpOrCancellation() }
                        if (upOrCancel == null) {
                            onLongClick()
                            waitForUpOrCancellation()
                        }
                    }
                }
            }
            .padding(S.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectionMode) {
            Box(
                modifier = Modifier
                    .size(D.avatarSm)
                    .background(
                        if (selected) Primary else Outline.copy(alpha = 0.3f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (selected) Text("✓", color = OnPrimary, fontWeight = FontWeight.Bold)
                else Text("○", color = Outline, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(S.md))
        }
        LCEmojiBadge(
            emoji = categoryEmojiOf(item.category),
            size = D.avatarMd,
            background = PrimaryContainer
        )
        Spacer(Modifier.width(S.md))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                item.displayName,
                style = MaterialTheme.typography.bodyLarge,
                color = Primary,
                fontWeight = FontWeight.Medium
            )
            Text(
                "${item.perServingKcal} kcal · 蛋白${formatG(item.perServingProtein)} · 脂肪${formatG(item.perServingFat)} · 碳水${formatG(item.perServingCarb)}",
                style = MaterialTheme.typography.labelSmall,
                color = Outline
            )
        }
    }
}

@Composable
private fun AddFoodDialog(
    onDismiss: () -> Unit,
    onConfirm: (com.lightcare.app.data.food.FoodLibraryRepository.AddFoodInput) -> Unit,
    title: String,
    initial: FoodItem? = null
) {
    var name by remember { mutableStateOf(initial?.displayName ?: "") }
    var cat by remember { mutableStateOf(initial?.category?.takeIf { it.isNotBlank() } ?: "其他") }
    var protein by remember { mutableStateOf(initial?.perServingProtein?.let { if (it > 0) it.toString().trimEnd('0').trimEnd('.') else "" } ?: "") }
    var fat by remember { mutableStateOf(initial?.perServingFat?.let { if (it > 0) it.toString().trimEnd('0').trimEnd('.') else "" } ?: "") }
    var carb by remember { mutableStateOf(initial?.perServingCarb?.let { if (it > 0) it.toString().trimEnd('0').trimEnd('.') else "" } ?: "") }
    var kcal by remember { mutableStateOf(initial?.perServingKcal?.toString() ?: "") }
    val cats = listOf("主食", "蛋白", "蔬果", "饮品", "其他")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(S.md)) {
                OutlinedTextField(
                    name,
                    { if (it.length <= 20) name = it },
                    label = { Text("食物名称（如：燕麦粥）") },
                    singleLine = true, shape = RoundedCornerShape(D.radiusSm),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(S.sm)) {
                    cats.forEach { c ->
                        val sel = cat == c
                        Box(
                            modifier = Modifier
                                .background(if (sel) Primary else PrimaryContainer, RoundedCornerShape(D.radiusPill))
                                .clickable { cat = c }
                                .padding(horizontal = S.md, vertical = S.xs)
                        ) {
                            Text(
                                c,
                                color = if (sel) OnPrimary else Primary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(S.sm), modifier = Modifier.fillMaxWidth()) {
                    NumField("蛋白 g", protein, { protein = it }, Modifier.weight(1f))
                    NumField("脂肪 g", fat, { fat = it }, Modifier.weight(1f))
                    NumField("碳水 g", carb, { carb = it }, Modifier.weight(1f))
                }
                NumField("热量 kcal", kcal, { kcal = it }, Modifier.fillMaxWidth())
                Text(
                    "蔬果份按分类自动设：蔬果=1，其余=0",
                    style = MaterialTheme.typography.labelSmall,
                    color = Outline
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val veg = if (cat == "蔬果") 1 else 0
                    onConfirm(
                        com.lightcare.app.data.food.FoodLibraryRepository.AddFoodInput(
                            displayName = name.trim().take(20),
                            category = cat,
                            perServingProtein = (protein.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 500.0),
                            perServingFat = (fat.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 500.0),
                            perServingCarb = (carb.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 500.0),
                            perServingKcal = (kcal.toIntOrNull() ?: 0).coerceIn(0, 5000),
                            perServingVeg = veg
                        )
                    )
                },
                enabled = name.isNotBlank() && kcal.isNotBlank()
            ) { Text(if (initial == null) "添加" else "保存", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun EditFoodDialog(
    item: FoodItem,
    onDismiss: () -> Unit,
    onSave: (com.lightcare.app.data.food.FoodLibraryRepository.AddFoodInput) -> Unit
) {
    AddFoodDialog(
        onDismiss = onDismiss,
        onConfirm = onSave,
        title = "编辑 ${item.displayName}",
        initial = item
    )
}

@Composable
private fun NumField(
    label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw ->
            val cleaned = raw.filter { it.isDigit() }.trimStart('0').let {
                if (it.isBlank() && raw.isNotEmpty()) "0" else it
            }
            onChange(cleaned.take(5))
        },
        label = { Text(label) },
        singleLine = true, shape = RoundedCornerShape(D.radiusSm),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}

private fun formatG(v: Double): String = if (v >= 100) "${v.toInt()}" else "%.1f".format(v)
