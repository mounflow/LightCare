package com.lightcare.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 全 App 间距 token（4 的倍数，MD3 节奏）。
 *
 * 命名采用 t-shirt size：所有页面只引 [Spacing.xxx]，
 * 禁写裸数字 `20.dp`，避免一处改间距全工程翻。
 */
data class LightCareSpacing(
    val none: Dp = 0.dp,
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 24.dp,
    val xxxl: Dp = 32.dp,
    val huge: Dp = 40.dp,
    val screenH: Dp = 20.dp,        // 屏幕左右内边距（主内容统一 20）
    val screenV: Dp = 20.dp,        // 屏幕上下内边距
    val cardPad: Dp = 20.dp,        // 卡片内边距（ambientCard 主体）
    val cardPadLg: Dp = 24.dp,      // 大卡片内边距
    val sectionGap: Dp = 20.dp,     // 同屏内卡片之间的呼吸
    val listGap: Dp = 12.dp,        // 列表项之间
    val tightGap: Dp = 8.dp,        // 紧凑列表
    val rowGap: Dp = 6.dp           // 行内元素
)

val LocalSpacing = staticCompositionLocalOf { LightCareSpacing() }
