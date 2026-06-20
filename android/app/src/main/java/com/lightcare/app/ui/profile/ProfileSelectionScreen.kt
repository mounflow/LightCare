package com.lightcare.app.ui.profile

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lightcare.app.data.api.ProfileDto
import com.lightcare.app.ui.theme.BorderSubtle
import com.lightcare.app.ui.theme.D
import com.lightcare.app.ui.theme.LCEmptyState
import com.lightcare.app.ui.theme.LCLoading
import com.lightcare.app.ui.theme.LCPrimaryButton
import com.lightcare.app.ui.theme.OnPrimary
import com.lightcare.app.ui.theme.Outline
import com.lightcare.app.ui.theme.Primary
import com.lightcare.app.ui.theme.PrimaryContainer
import com.lightcare.app.ui.theme.S

/**
 * 选档 / 建档页（深度优化版）。
 *
 * 视觉层次：
 * - 顶部品牌区（emoji + 名称 + 副标题）
 * - 状态切换：loading / error / create / list
 * - 列表项：圆形头像 + 姓名 + 关系 + "进入 →"
 * - "+ 新建档案" 胶囊
 */
@Composable
fun ProfileSelectionScreen(vm: ProfileSelectionViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 顶栏品牌区
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = S.xxl, vertical = S.huge)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🌿", style = MaterialTheme.typography.displayMedium)
                Spacer(Modifier.size(S.md))
                Column {
                    Text(
                        "轻养 LightCare",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(S.xxs))
                    Text(
                        "选择一份档案开始今天的轻养",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Outline
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.loading -> LCLoading()
                state.error != null && state.profiles.isEmpty() -> LCEmptyState(
                    emoji = "😅",
                    message = state.error!!,
                    actionLabel = "重试",
                    onAction = { vm.load() }
                )
                state.profiles.isEmpty() -> CreateCard(busy = state.busy) { n, p ->
                    vm.createFirst(n, p.birthDate, p.gender, p.heightCm, p.weightKg, p.activityLevel)
                }
                else -> ProfileList(
                    profiles = state.profiles,
                    busy = state.busy,
                    onSelect = { vm.select(it) },
                    onCreate = { n, p ->
                        vm.createFirst(n, p.birthDate, p.gender, p.heightCm, p.weightKg, p.activityLevel)
                    }
                )
            }

            state.error?.takeIf { state.profiles.isNotEmpty() }?.let { err ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(S.xl)
                ) {
                    Text(
                        err,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateCard(busy: Boolean, onCreate: (String, PhysiqueInput) -> Unit) {
    var name by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val phys = remember { PhysiqueInputState() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = S.xxl, vertical = S.lg),
        verticalArrangement = Arrangement.spacedBy(S.lg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(D.radiusXl))
                .border(D.hairline, BorderSubtle, RoundedCornerShape(D.radiusXl))
                .padding(S.xxl),
            verticalArrangement = Arrangement.spacedBy(S.lg)
        ) {
            Text(
                "创建你的第一份档案",
                style = MaterialTheme.typography.titleLarge,
                color = Primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "档案用来记录饮食与作息。之后还能为家人再建（最多 4 份）。",
                style = MaterialTheme.typography.bodySmall,
                color = Outline
            )
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 20) name = it },
                label = { Text("称呼（如：我、妈妈）") },
                singleLine = true,
                shape = RoundedCornerShape(D.radiusMd),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (expanded) "收起更多信息" else "📐 填身体数据（可选，让目标更准）",
                    style = MaterialTheme.typography.labelMedium,
                    color = Primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (expanded) {
                PhysiqueFields(phys)
            }
            LCPrimaryButton(
                text = "开始",
                onClick = { onCreate(name, phys.snapshot()) },
                enabled = name.isNotBlank() && !busy,
                loading = busy
            )
        }
    }
}

private class PhysiqueInputState {
    var height by mutableStateOf("")
    var weight by mutableStateOf("")
    var birthYear by mutableStateOf("")
    var gender by mutableStateOf("U")
    var activity by mutableStateOf("LIGHT")
    fun snapshot() = PhysiqueInput(
        heightCm = height.toIntOrNull()?.coerceIn(50, 300),
        weightKg = weight.replace(",", ".").toDoubleOrNull()?.coerceIn(10.0, 500.0),
        birthDate = birthYear.trim().toIntOrNull()
            ?.coerceIn(1900, java.time.LocalDate.now().year)?.let { "$it-01-01" },
        gender = gender.takeIf { it.isNotBlank() },
        activityLevel = activity.takeIf { it.isNotBlank() }
    )
}
private data class PhysiqueInput(
    val heightCm: Int? = null,
    val weightKg: Double? = null,
    val birthDate: String? = null,
    val gender: String? = null,
    val activityLevel: String? = null
)

@Composable
private fun PhysiqueFields(phys: PhysiqueInputState) {
    Column(verticalArrangement = Arrangement.spacedBy(S.md)) {
        Row(horizontalArrangement = Arrangement.spacedBy(S.sm)) {
            NumField("身高 cm", phys.height, { phys.height = it }, Modifier.weight(1f))
            NumField("体重 kg", phys.weight, { phys.weight = it }, Modifier.weight(1f))
        }
        NumField("出生年份", phys.birthYear, { phys.birthYear = it }, Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(S.xs)) {
            listOf("M" to "男", "F" to "女", "U" to "不填").forEach { (k, l) ->
                val sel = phys.gender == k
                Box(
                    modifier = Modifier
                        .background(if (sel) Primary else PrimaryContainer, RoundedCornerShape(D.radiusPill))
                        .clickable { phys.gender = k }
                        .padding(horizontal = S.md, vertical = S.xs)
                ) {
                    Text(
                        l,
                        color = if (sel) OnPrimary else Primary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(S.xs)) {
            listOf(
                "SEDENTARY" to "久坐",
                "LIGHT" to "轻度",
                "MODERATE" to "中度",
                "ACTIVE" to "高度",
                "VERY_ACTIVE" to "极高"
            ).forEach { (k, l) ->
                val sel = phys.activity == k
                Box(
                    modifier = Modifier
                        .background(if (sel) Primary else PrimaryContainer, RoundedCornerShape(D.radiusPill))
                        .clickable { phys.activity = k }
                        .padding(horizontal = S.sm, vertical = S.xs)
                ) {
                    Text(
                        l,
                        color = if (sel) OnPrimary else Primary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
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

@Composable
private fun ProfileList(
    profiles: List<ProfileDto>,
    busy: Boolean,
    onSelect: (ProfileDto) -> Unit,
    onCreate: (String, PhysiqueInput) -> Unit
) {
    var creating by remember { mutableStateOf(false) }

    if (creating) {
        CreateCard(busy = busy) { n, p -> onCreate(n, p); creating = false }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = S.xxl, vertical = S.lg),
        verticalArrangement = Arrangement.spacedBy(S.md)
    ) {
        items(profiles, key = { it.id }) { p ->
            ProfileRow(p) { onSelect(p) }
        }
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PrimaryContainer, RoundedCornerShape(D.radiusLg))
                    .clickable(enabled = !busy && profiles.size < 4) { creating = true }
                    .padding(S.xl),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "＋ 新建档案",
                    color = Primary,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            if (profiles.size >= 4) {
                Spacer(Modifier.height(S.xs))
                Text(
                    "已达 4 份上限",
                    style = MaterialTheme.typography.labelSmall,
                    color = Outline,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ProfileRow(p: ProfileDto, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(D.radiusLg))
            .border(D.hairline, BorderSubtle, RoundedCornerShape(D.radiusLg))
            .clickable(onClick = onClick)
            .padding(S.xl),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(S.lg)
    ) {
        Box(
            modifier = Modifier
                .size(D.avatarLg)
                .background(PrimaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                Icons.Outlined.Person,
                contentDescription = null,
                tint = Primary
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                p.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = Primary,
                fontWeight = FontWeight.SemiBold
            )
            val sub = when (p.relation) {
                "SELF" -> "本人"
                "FAMILY_OTHER" -> "家人"
                else -> p.relation
            }
            Text(sub, style = MaterialTheme.typography.bodySmall, color = Outline)
        }
        Text(
            "进入 →",
            style = MaterialTheme.typography.labelLarge,
            color = Primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}
