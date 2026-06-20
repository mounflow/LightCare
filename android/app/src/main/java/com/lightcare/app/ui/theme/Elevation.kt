package com.lightcare.app.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 温暖圆润风的"层级"——靠柔阴影 + 大圆角 + 暖色描边，而非 1px 冷线框。
 *
 * 历史命名（wireframe / ambientCard / ambientSoft / ambientStrong / surgicalAccent）
 * 被 23+ 处引用，函数签名沿用，语义已从"线框/直角"翻转为"柔阴影/圆角"。
 * 调用点无需改动即可自动获得卡通圆润观感。
 *
 * - wireframe()      → 暖色 1px 描边 + 16dp 圆角（轻容器）
 * - ambientCard      → 柔阴影 + 暖色描边 + 24dp 圆角（卡片主容器）
 * - ambientSoft      → 同 wireframe（轻分隔）
 * - ambientStrong    → 暖橙 1.5dp 描边（激活/强调）
 * - surgicalAccent   → 左侧柔绿圆角胶囊条（正向/健康数据卡）
 *
 * 注：真实阴影通过 Modifier.shadow() 实现，描边用 border()，
 *     两者可叠加（先阴影再描边），Compose 渲染顺序天然支持。
 */

/** 卡片圆角形状（与 Theme Shapes.large 对齐） */
private val CardShape = RoundedCornerShape(24.dp)
private val SoftShape = RoundedCornerShape(16.dp)

/** 柔和扩散阴影的颜色（暖灰半透明，非纯黑，更温柔） */
private val SoftShadowColor = Color(0x1A8A6B4A)

/**
 * 暖色描边 + 圆角（轻容器用）。
 */
@Composable
fun Modifier.wireframe(
    width: Dp = 1.dp,
    color: Color = BorderSubtle,
    shape: Shape = SoftShape
): Modifier = this.border(width = width, color = color, shape = shape)

/**
 * 卡片：柔阴影 + 暖色描边 + 24dp 圆角。
 * 这是全 App 最常见的容器形态，直接决定"圆润可爱"基调。
 */
@Composable
fun Modifier.ambientCard(): Modifier = this
    .shadow(
        elevation = 6.dp,
        shape = CardShape,
        ambientColor = SoftShadowColor,
        spotColor = SoftShadowColor
    )
    .border(width = 1.dp, color = BorderSubtle, shape = CardShape)

/** 轻分隔：暖色描边 + 16dp 圆角 */
@Composable
fun Modifier.ambientSoft(): Modifier = wireframe()

/** 强调：暖橙 1.5dp 描边 + 16dp 圆角（激活态） */
@Composable
fun Modifier.ambientStrong(): Modifier =
    wireframe(width = 1.5.dp, color = BorderStrong)

/**
 * 柔绿左圆角胶囊条（正向 / 健康 / 推荐数据卡）。
 * 旧版画的是 2px 直线（冷峻），现在画一个左侧圆角胶囊条（圆润可爱）。
 *
 * 配合 ambientCard 一起用时：卡片本身已是 24dp 圆角 + 阴影，
 * 这里在内部左侧叠一条柔绿胶囊，作为"健康/正向"的彩色点缀。
 */
@Composable
fun Modifier.surgicalAccent(
    width: Dp = 5.dp,
    color: Color = SurgicalGreen
): Modifier = this.drawBehind {
    val wPx = with(density) { width.toPx() }
    val rPx = wPx / 2f
    // 左侧胶囊：一个窄长条的圆角矩形，紧贴左边缘，纵向居中留出上下呼吸
    drawRoundRect(
        color = color,
        topLeft = Offset(0f, size.height * 0.18f),
        size = Size(wPx, size.height * 0.64f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(rPx, rPx)
    )
}
