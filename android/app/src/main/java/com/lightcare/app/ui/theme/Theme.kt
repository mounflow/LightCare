package com.lightcare.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightCareColors = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = NutrientWater,                 // 水分 = 第三语义色
    onTertiary = OnPrimary,
    tertiaryContainer = Color(0xFFE1F5FE),
    onTertiaryContainer = Color(0xFF01579B),
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    outline = Outline,
    outlineVariant = OutlineVariant,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer
)

/**
 * 温暖圆润风：形状语言是"软糯、可爱"。
 * - 卡片 / 大容器 24dp（胖嘟嘟的圆）
 * - 按钮 / 输入 16dp
 * - 进度条 / chip 用胶囊（50% 圆角）
 */
private val LightCareShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),       // 卡片
    extraLarge = RoundedCornerShape(28.dp)   // 大容器 / sheet
)

/** 统一 token 实例。 */
val LightCareDimensDefault = LightCareDimens()
val LightCareSpacingDefault = LightCareSpacing()

@Composable
fun LightCareTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalSpacing provides LightCareSpacingDefault,
        LocalDimens provides LightCareDimensDefault
    ) {
        MaterialTheme(
            colorScheme = LightCareColors,
            typography = LightCareTypography,
            shapes = LightCareShapes,
            content = content
        )
    }
}

/** 取当前 Composable scope 下的 spacing（缩写字面量，让调用方写 `S.lg` 而非 `LocalSpacing.current.lg`）。 */
object S {
    val none
        @Composable get() = LocalSpacing.current.none
    val xxs  @Composable get() = LocalSpacing.current.xxs
    val xs   @Composable get() = LocalSpacing.current.xs
    val sm   @Composable get() = LocalSpacing.current.sm
    val md   @Composable get() = LocalSpacing.current.md
    val lg   @Composable get() = LocalSpacing.current.lg
    val xl   @Composable get() = LocalSpacing.current.xl
    val xxl  @Composable get() = LocalSpacing.current.xxl
    val xxxl @Composable get() = LocalSpacing.current.xxxl
    val huge @Composable get() = LocalSpacing.current.huge
    val screenH   @Composable get() = LocalSpacing.current.screenH
    val screenV   @Composable get() = LocalSpacing.current.screenV
    val cardPad   @Composable get() = LocalSpacing.current.cardPad
    val cardPadLg @Composable get() = LocalSpacing.current.cardPadLg
    val sectionGap @Composable get() = LocalSpacing.current.sectionGap
    val listGap   @Composable get() = LocalSpacing.current.listGap
    val tightGap  @Composable get() = LocalSpacing.current.tightGap
    val rowGap    @Composable get() = LocalSpacing.current.rowGap
}

object D {
    val radiusXs @Composable get() = LocalDimens.current.radiusXs
    val radiusSm @Composable get() = LocalDimens.current.radiusSm
    val radiusMd @Composable get() = LocalDimens.current.radiusMd
    val radiusLg @Composable get() = LocalDimens.current.radiusLg
    val radiusXl @Composable get() = LocalDimens.current.radiusXl
    val radiusXxl @Composable get() = LocalDimens.current.radiusXxl
    val radiusPill @Composable get() = LocalDimens.current.radiusPill
    val hairline  @Composable get() = LocalDimens.current.hairline
    val stroke    @Composable get() = LocalDimens.current.stroke
    val thick     @Composable get() = LocalDimens.current.thick
    val accentBar @Composable get() = LocalDimens.current.accentBar
    val controlSm @Composable get() = LocalDimens.current.controlSm
    val controlMd @Composable get() = LocalDimens.current.controlMd
    val controlLg @Composable get() = LocalDimens.current.controlLg
    val controlXl @Composable get() = LocalDimens.current.controlXl
    val bottomBar @Composable get() = LocalDimens.current.bottomBar
    val topBar    @Composable get() = LocalDimens.current.topBar
    val avatarSm  @Composable get() = LocalDimens.current.avatarSm
    val avatarMd  @Composable get() = LocalDimens.current.avatarMd
    val avatarLg  @Composable get() = LocalDimens.current.avatarLg
    val avatarXl  @Composable get() = LocalDimens.current.avatarXl
    val iconHero  @Composable get() = LocalDimens.current.iconHero
    val progressTrack   @Composable get() = LocalDimens.current.progressTrack
    val progressTrackLg @Composable get() = LocalDimens.current.progressTrackLg
    val ringSize @Composable get() = LocalDimens.current.ringSize
    val shadowSm @Composable get() = LocalDimens.current.shadowSm
    val shadowMd @Composable get() = LocalDimens.current.shadowMd
    val shadowLg @Composable get() = LocalDimens.current.shadowLg
}
