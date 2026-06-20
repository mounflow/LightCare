package com.lightcare.app.ui.shared

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lightcare.app.ui.theme.D
import com.lightcare.app.ui.theme.Motion
import com.lightcare.app.ui.theme.OnPrimary
import com.lightcare.app.ui.theme.Outline
import com.lightcare.app.ui.theme.Primary
import com.lightcare.app.ui.theme.PrimaryContainer
import com.lightcare.app.ui.theme.S
import com.lightcare.app.ui.theme.SurgicalGreen

/**
 * 底部导航（深度优化版）。
 *
 * - 4 个位置：主页 / 记录（SurgicalGreen 大圆 FAB） / 数据 / 设置
 * - 选中 = PrimaryContainer 药丸底 + 缩放 1.05
 * - 未选中 = 透明 + Outline 灰
 * - 切换有短动效（125ms 缩放 + tint）
 */
enum class MainTab(val icon: ImageVector, val label: String) {
    Home(Icons.Outlined.Home, "主页"),
    Data(Icons.Outlined.Analytics, "数据"),
    Settings(Icons.Outlined.Settings, "设置")
}

@Composable
fun MainScaffold(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    onFabClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.weight(1f)) { content() }
        BottomBar(
            currentTab = currentTab,
            onTabSelected = onTabSelected,
            onAddClick = onFabClick
        )
    }
}

@Composable
private fun BottomBar(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    onAddClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(D.bottomBar)
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = S.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        TabItem(
            tab = MainTab.Home, selected = currentTab == MainTab.Home,
            onClick = { onTabSelected(MainTab.Home) },
            modifier = Modifier.weight(1f)
        )
        AddTab(onAddClick, Modifier.weight(1f))
        TabItem(
            tab = MainTab.Data, selected = currentTab == MainTab.Data,
            onClick = { onTabSelected(MainTab.Data) },
            modifier = Modifier.weight(1f)
        )
        TabItem(
            tab = MainTab.Settings, selected = currentTab == MainTab.Settings,
            onClick = { onTabSelected(MainTab.Settings) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TabItem(
    tab: MainTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val targetScale = if (selected) 1.05f else 1f
    val scale by animateFloatAsState(targetScale, tween(Motion.SHORT), label = "tabScale")
    val tint = if (selected) Primary else Outline
    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onClick)
            .padding(vertical = S.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .background(
                    if (selected) PrimaryContainer else Color.Transparent,
                    RoundedCornerShape(D.radiusPill)
                )
                .padding(horizontal = S.lg, vertical = S.xs),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size((22 * scale).dp)
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    tab.icon,
                    contentDescription = tab.label,
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(Modifier.height(S.xxs))
        Text(
            tab.label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun AddTab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val targetScale = if (false) 0.95f else 1f  // 留位：按下缩放
    val scale by animateFloatAsState(targetScale, tween(Motion.SHORT), label = "addScale")
    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size((44 * scale).dp)
                .background(SurgicalGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Add,
                contentDescription = "添加",
                tint = OnPrimary,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.height(S.xxs))
        Text("记录", style = MaterialTheme.typography.labelSmall, color = Outline)
    }
}
