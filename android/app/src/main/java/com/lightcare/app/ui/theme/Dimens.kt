package com.lightcare.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 尺寸 / 圆角 / 描边 / 高度 token。
 *
 * 改尺寸不再去各 Screen 翻硬编码，全部走 [LightCareDimens]。
 */
data class LightCareDimens(
    // —— 圆角 ——
    val radiusXs: Dp = 8.dp,         // tag / 小元素
    val radiusSm: Dp = 12.dp,        // 输入框 / chip
    val radiusMd: Dp = 16.dp,        // 按钮 / 中型卡
    val radiusLg: Dp = 20.dp,        // 中型容器
    val radiusXl: Dp = 24.dp,        // 大卡片（ambientCard 主体）
    val radiusXxl: Dp = 28.dp,       // sheet / 大容器
    val radiusPill: Dp = 50.dp,      // 胶囊（50% 圆角在 Compose 写法里 50.dp 即圆形）
    val radiusDot: Dp = 50.dp,       // 圆点

    // —— 描边 ——
    val hairline: Dp = 1.dp,
    val stroke: Dp = 1.5.dp,
    val thick: Dp = 2.dp,
    val accentBar: Dp = 5.dp,        // surgicalAccent 宽度

    // —— 高度 ——
    val controlSm: Dp = 36.dp,       // 小按钮 / stepper
    val controlMd: Dp = 44.dp,       // 中按钮 / 输入框
    val controlLg: Dp = 48.dp,       // 大按钮
    val controlXl: Dp = 52.dp,       // 主 CTA
    val bottomBar: Dp = 64.dp,       // 底部导航
    val topBar: Dp = 56.dp,          // 顶栏

    // —— 头像 / 圆形 ——
    val avatarSm: Dp = 32.dp,
    val avatarMd: Dp = 40.dp,
    val avatarLg: Dp = 48.dp,
    val avatarXl: Dp = 64.dp,
    val iconHero: Dp = 96.dp,        // PickEntry 圆

    // —— 进度 / 环 ——
    val progressTrack: Dp = 8.dp,    // 细胶囊
    val progressTrackLg: Dp = 14.dp, // 粗胶囊（余量条）
    val ringSize: Dp = 96.dp,        // 大圆环

    // —— 阴影 ——
    val shadowSm: Dp = 3.dp,
    val shadowMd: Dp = 6.dp,
    val shadowLg: Dp = 10.dp
)

val LocalDimens = staticCompositionLocalOf { LightCareDimens() }
