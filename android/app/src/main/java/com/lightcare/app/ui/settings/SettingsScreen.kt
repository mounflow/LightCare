package com.lightcare.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lightcare.app.ui.theme.D
import com.lightcare.app.ui.theme.LCStatCell
import com.lightcare.app.ui.theme.LCTopBar
import com.lightcare.app.ui.theme.Outline
import com.lightcare.app.ui.theme.Primary
import com.lightcare.app.ui.theme.PrimaryContainer
import com.lightcare.app.ui.theme.S
import com.lightcare.app.ui.theme.ambientCard
import com.lightcare.app.ui.theme.surgicalAccent

/**
 * 设置页（深度优化版）。
 *
 * - 顶栏用 LCTopBar
 * - 当前档案卡：柔绿左条 + 5 个目标 cell
 * - 入口卡：图标 badge + 标题 + 副标题 + 箭头
 */
@Composable
fun SettingsScreen(
    onSwitchProfile: () -> Unit = {},
    onNavigateToFoodLibrary: () -> Unit = {},
    onNavigateToPhysique: () -> Unit = {},
    vm: SettingsViewModel = hiltViewModel()
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    val ctx = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LCTopBar(title = "设置", emoji = "⚙️")
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = S.xxxl),
            verticalArrangement = Arrangement.spacedBy(S.md)
        ) {
            item { Box(Modifier.padding(horizontal = S.screenH)) { CurrentProfileCard() } }
            item {
                Box(Modifier.padding(horizontal = S.screenH)) {
                    EntryCard(
                        emoji = "📐", title = "我的身体数据",
                        subtitle = "身高/体重/年龄/性别/活动量",
                        onClick = onNavigateToPhysique
                    )
                }
            }
            item {
                Box(Modifier.padding(horizontal = S.screenH)) {
                    EntryCard(
                        emoji = "🔄", title = "切换 / 退出档案",
                        subtitle = "选择另一份档案，或重新建档",
                        onClick = onSwitchProfile
                    )
                }
            }
            item {
                Box(Modifier.padding(horizontal = S.screenH)) {
                    EntryCard(
                        emoji = "📚", title = "我的食物库",
                        subtitle = "管理内置与自定义食物",
                        onClick = onNavigateToFoodLibrary
                    )
                }
            }
            // PR-Auth：退出登录（清 server 端会话 + 本地账号 + 本地缓存）
            item {
                Box(Modifier.padding(horizontal = S.screenH)) {
                    EntryCard(
                        emoji = "🚪", title = "退出登录",
                        subtitle = "清除本机账号与缓存，下次打开需重新登录",
                        onClick = { showLogoutDialog = true }
                    )
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("确定要退出登录？", fontWeight = FontWeight.SemiBold) },
            text = { Text("会清掉本机所有食物和记录缓存（服务器端的数据保留，重新登录即可看到）。") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    vm.logout {
                        android.widget.Toast.makeText(ctx, "已退出登录", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }) { Text("退出", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun CurrentProfileCard() {
    val vm: com.lightcare.app.ui.profile.PhysiqueViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val p = state.profile ?: return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .ambientCard()
            .surgicalAccent()
            .padding(S.xl),
        verticalArrangement = Arrangement.spacedBy(S.lg)
    ) {
        com.lightcare.app.ui.theme.LCCardLabel("当前档案", emoji = "🌿")
        Text(
            p.displayName,
            style = MaterialTheme.typography.headlineSmall,
            color = Primary,
            fontWeight = FontWeight.Bold
        )
        // 生理信息一行（性别/年龄/身高/体重），缺字段用占位
        val genderText = when (p.gender?.uppercase()) { "M" -> "男"; "F" -> "女"; else -> "未填" }
        val ageText = p.birthDate?.let {
            val birthYear = it.take(4).toIntOrNull() ?: return@let null
            (java.time.LocalDate.now().year - birthYear).coerceAtLeast(0)
        }?.let { "$it 岁" }
        val physique = listOfNotNull(
            genderText,
            ageText,
            p.heightCm?.let { "${it} cm" },
            p.weightKg?.let { "${it.toInt()} kg" }
        ).joinToString(" · ").ifBlank { "未填写身体数据" }
        Text(physique, style = MaterialTheme.typography.bodyMedium, color = Outline)

        Text(
            "每日目标（按 Mifflin-St Jeor 计算）",
            style = MaterialTheme.typography.labelMedium,
            color = Outline
        )
        // 目标网格：热量/蛋白/水分/蔬果/步数。target 为 null → 显示"—"（用户没填身体数据）。
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(S.sm)) {
            LCStatCell("热量", "${p.calorieTargetKcal ?: "—"}", "kcal", emoji = "🔥", modifier = Modifier.weight(1f))
            LCStatCell("蛋白", "${p.proteinTargetG ?: "—"}", "g", emoji = "💪", modifier = Modifier.weight(1f))
            LCStatCell("水分", "${(p.waterTargetMl ?: 0) / 250}", "杯", emoji = "💧", modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(S.sm)) {
            LCStatCell("蔬果", "${p.vegTargetServings}", "份", emoji = "🥦", modifier = Modifier.weight(1f))
            LCStatCell("步数", "${(p.stepTarget ?: 0) / 1000}", "千步", emoji = "👟", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun EntryCard(
    emoji: String, title: String, subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .ambientCard()
            .clickable(onClick = onClick)
            .padding(S.xl),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(D.avatarLg)
                .background(PrimaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, style = MaterialTheme.typography.titleLarge)
        }
        androidx.compose.foundation.layout.Spacer(Modifier.padding(horizontal = S.md))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = Primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Outline
            )
        }
        androidx.compose.material3.Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(24.dp)
        )
    }
}
