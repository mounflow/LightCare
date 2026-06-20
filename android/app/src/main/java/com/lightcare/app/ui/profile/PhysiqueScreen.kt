package com.lightcare.app.ui.profile

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lightcare.app.data.api.ProfileDto
import com.lightcare.app.ui.theme.D
import com.lightcare.app.ui.theme.LCEmptyState
import com.lightcare.app.ui.theme.LCLoading
import com.lightcare.app.ui.theme.LCStatCell
import com.lightcare.app.ui.theme.LCTopBar
import com.lightcare.app.ui.theme.OnPrimary
import com.lightcare.app.ui.theme.Outline
import com.lightcare.app.ui.theme.Primary
import com.lightcare.app.ui.theme.PrimaryContainer
import com.lightcare.app.ui.theme.S
import com.lightcare.app.ui.theme.ambientCard
import java.time.LocalDate

/**
 * 我的身体数据页（深度优化版）。
 */
@Composable
fun PhysiqueScreen(
    onBack: () -> Unit,
    vm: PhysiqueViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showEditor by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LCTopBar(title = "我的身体数据", onBack = onBack, emoji = "📐")

        when {
            state.profile == null && state.loading -> LCLoading()
            state.profile == null && state.error -> LCEmptyState(
                emoji = "😅",
                message = "加载失败，请确认本地服务已启动",
                actionLabel = "重试",
                onAction = { vm.reload() }
            )
            state.profile == null -> LCEmptyState(emoji = "📐", message = "暂无身体数据")
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentPadding = PaddingValues(
                        horizontal = S.screenH,
                        vertical = S.lg
                    )
                ) {
                    item { PhysiqueOverviewCard(state.profile!!, onEdit = { showEditor = true }) }
                }
            }
        }
    }

    if (showEditor && state.profile != null) {
        PhysiqueEditorDialog(
            profile = state.profile!!,
            saving = state.saving,
            onDismiss = { showEditor = false },
            onSave = { bd, g, h, w, act ->
                vm.updatePhysique(bd, g, h, w, act)
                showEditor = false
            }
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
private fun PhysiqueOverviewCard(p: ProfileDto, onEdit: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .ambientCard()
            .padding(S.xl),
        verticalArrangement = Arrangement.spacedBy(S.lg)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            com.lightcare.app.ui.theme.LCCardLabel("基本信息", emoji = "📋")
            Box(
                modifier = Modifier
                    .background(Primary, RoundedCornerShape(D.radiusPill))
                    .clickable(onClick = onEdit)
                    .padding(horizontal = S.lg, vertical = S.xs)
            ) {
                Text(
                    "编辑",
                    color = OnPrimary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(S.md)
        ) {
            PhysiqueBigCell("📏 身高", p.heightCm?.let { "${it}" } ?: "—", "cm", Modifier.weight(1f))
            PhysiqueBigCell("⚖️ 体重", p.weightKg?.let { "%.1f".format(it) } ?: "—", "kg", Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(S.md)
        ) {
            val age = p.birthDate?.let {
                val y = LocalDate.now().year - LocalDate.parse(it).year
                "$y"
            } ?: "—"
            PhysiqueBigCell("🎂 年龄", age, "岁", Modifier.weight(1f))
            PhysiqueBigCell(
                "👤 性别",
                when (p.gender) {
                    "M" -> "男"; "F" -> "女"; "U" -> "—"; else -> "—"
                },
                "",
                Modifier.weight(1f)
            )
        }
        PhysiqueBigCell(
            "🏃 活动量",
            when (p.activityLevel) {
                "SEDENTARY" -> "久坐"; "LIGHT" -> "轻度"
                "MODERATE" -> "中度"; "ACTIVE" -> "高度"; "VERY_ACTIVE" -> "极高"
                else -> "—"
            },
            "",
            Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PhysiqueBigCell(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(PrimaryContainer, RoundedCornerShape(D.radiusMd))
            .padding(vertical = S.lg, horizontal = S.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(S.xs)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Outline)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                color = Primary,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            if (unit.isNotBlank()) {
                Spacer(Modifier.size(S.xxs))
                Text(
                    unit,
                    style = MaterialTheme.typography.labelMedium,
                    color = Outline,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }
    }
}

@Composable
fun PhysiqueEditorDialog(
    profile: ProfileDto,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (birthDate: String?, gender: String?, heightCm: Int?, weightKg: Double?, activityLevel: String?) -> Unit
) {
    var height by remember { mutableStateOf(profile.heightCm?.toString() ?: "") }
    var weight by remember { mutableStateOf(profile.weightKg?.let { "%.1f".format(it) } ?: "") }
    var birthYear by remember { mutableStateOf(profile.birthDate?.take(4) ?: "") }
    var gender by remember { mutableStateOf(profile.gender ?: "U") }
    var activity by remember { mutableStateOf(profile.activityLevel ?: "LIGHT") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑身体数据", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(S.md)) {
                Text(
                    "用于 Mifflin-St Jeor 公式估算 BMR/TDEE，生成每日目标。",
                    style = MaterialTheme.typography.labelSmall,
                    color = Outline
                )
                Row(horizontalArrangement = Arrangement.spacedBy(S.sm)) {
                    NumField("身高 cm", height, { height = it }, Modifier.weight(1f))
                    NumField("体重 kg", weight, { weight = it }, Modifier.weight(1f))
                }
                NumField("出生年份 (例 1990)", birthYear, { birthYear = it }, Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(S.xs)) {
                    listOf("M" to "男", "F" to "女", "U" to "不填").forEach { (k, label) ->
                        val sel = gender == k
                        Box(
                            modifier = Modifier
                                .background(if (sel) Primary else PrimaryContainer, RoundedCornerShape(D.radiusPill))
                                .clickable { gender = k }
                                .padding(horizontal = S.md, vertical = S.xs)
                        ) {
                            Text(
                                label,
                                color = if (sel) androidx.compose.ui.graphics.Color.White else Primary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Column {
                    Text("活动量", style = MaterialTheme.typography.labelMedium, color = Outline)
                    Spacer(Modifier.height(S.xs))
                    Row(horizontalArrangement = Arrangement.spacedBy(S.xs)) {
                        listOf(
                            "SEDENTARY" to "久坐",
                            "LIGHT" to "轻度",
                            "MODERATE" to "中度",
                            "ACTIVE" to "高度",
                            "VERY_ACTIVE" to "极高"
                        ).forEach { (k, label) ->
                            val sel = activity == k
                            Box(
                                modifier = Modifier
                                    .background(if (sel) Primary else PrimaryContainer, RoundedCornerShape(D.radiusPill))
                                    .clickable { activity = k }
                                    .padding(horizontal = S.md, vertical = S.xs)
                            ) {
                                Text(
                                    label,
                                    color = if (sel) androidx.compose.ui.graphics.Color.White else Primary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val h = height.toIntOrNull()?.coerceIn(50, 300)
                    val w = weight.replace(",", ".").toDoubleOrNull()?.coerceIn(10.0, 500.0)
                    val thisYear = java.time.LocalDate.now().year
                    val bd = birthYear.trim().toIntOrNull()?.coerceIn(1900, thisYear)
                        ?.let { y -> "$y-01-01" }
                    onSave(bd, gender, h, w, activity)
                },
                enabled = !saving
            ) { Text(if (saving) "保存中..." else "保存", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun NumField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
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
