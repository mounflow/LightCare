package com.lightcare.app.ui.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp as DpT

/**
 * 共享 UI 组件（温暖圆润风）。
 *
 * 把 4 个 Screen 都在写的"小标签卡 / 空态 / 顶栏 / 主按钮 / 营养 chip" 等
 * 抽象出来 —— 改一处全工程同步，避免每个 Screen 写法漂移。
 *
 * 设计原则：
 * - 走 [S] / [D] token，不写裸 dp。
 * - 颜色一律走 [MaterialTheme.colorScheme] / token 颜色，禁硬编 hex。
 * - 触摸反馈：所有可点元素带 alpha 0.6 压暗 + scale 0.98 收缩。
 */

// ───────────────────────────────────────────────────────────────
// 1. 通用顶栏：返回 + 居中标题 + 右侧动作
// ───────────────────────────────────────────────────────────────

@Composable
fun LCTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    emoji: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = S.sm, vertical = S.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            Box(
                modifier = Modifier
                    .size(D.topBar - 16.dp)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "返回",
                    tint = Primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            Spacer(Modifier.width(D.topBar - 16.dp))
        }
        Text(
            text = if (emoji != null) "$emoji $title" else title,
            style = MaterialTheme.typography.headlineSmall,
            color = Primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = S.sm),
            textAlign = TextAlign.Center
        )
        Row(verticalAlignment = Alignment.CenterVertically) { actions() }
    }
}

// ───────────────────────────────────────────────────────────────
// 2. 状态展示：Loading / Empty / Error（统一风格）
// ───────────────────────────────────────────────────────────────

@Composable
fun LCLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            color = Primary,
            strokeWidth = D.thick,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
fun LCEmptyState(
    emoji: String = "📋",
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(S.huge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(emoji, style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(S.md))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = Outline,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(S.md))
            TextButton(onClick = onAction) {
                Text(actionLabel, color = Primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ───────────────────────────────────────────────────────────────
// 3. 主按钮：填充式（Primary / Error 二选一）
// ───────────────────────────────────────────────────────────────

@Composable
fun LCPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingEmoji: String? = null
) {
    val bg = if (enabled) Primary else Outline
    val targetScale = if (enabled) 1f else 0.98f
    val scale by animateFloatAsState(targetScale, tween(Motion.SHORT), label = "btnScale")
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(D.controlXl)
            .scale(scale)
            .background(bg, RoundedCornerShape(D.radiusMd))
            .clickable(enabled = enabled && !loading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = OnPrimary,
                strokeWidth = D.thick,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leadingEmoji != null) {
                    Text(leadingEmoji, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(S.sm))
                }
                Text(
                    text,
                    color = OnPrimary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
fun LCSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingEmoji: String? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(D.controlLg)
            .background(PrimaryContainer, RoundedCornerShape(D.radiusMd))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingEmoji != null) {
                Text(leadingEmoji, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.width(S.sm))
            }
            Text(
                text,
                color = Primary,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}

// ───────────────────────────────────────────────────────────────
// 4. 营养小卡 chip：竖排（label / value+unit），
//    用于首页"今日余量"、周报"达标环"、设置"目标"、周报"统计单元" 等。
// ───────────────────────────────────────────────────────────────

@Composable
fun LCStatCell(
    label: String,
    value: String,
    unit: String? = null,
    emoji: String? = null,
    emphasize: Boolean = false,
    modifier: Modifier = Modifier
) {
    val bg = if (emphasize) Primary else PrimaryContainer
    val fg = if (emphasize) OnPrimary else OnPrimaryContainer
    Column(
        modifier = modifier
            .background(bg, RoundedCornerShape(D.radiusMd))
            .padding(horizontal = S.md, vertical = S.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (emoji != null) {
            Text(emoji, style = MaterialTheme.typography.bodyLarge)
        } else {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = fg.copy(alpha = 0.7f),
                fontWeight = FontWeight.SemiBold
            )
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                color = fg,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            if (!unit.isNullOrBlank()) {
                Spacer(Modifier.width(2.dp))
                Text(
                    unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = fg.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }
        if (emoji != null) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = fg.copy(alpha = 0.85f)
            )
        }
    }
}

// ───────────────────────────────────────────────────────────────
// 5. 进度胶囊：track + fill
// ───────────────────────────────────────────────────────────────

@Composable
fun LCCapsuleProgress(
    progress: Float,                          // 0..1
    fillColor: Color = SurgicalGreen,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = D.progressTrackLg
) {
    val safe = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = safe,
        animationSpec = tween(Motion.LONG, easing = Motion.Emphasized),
        label = "capsuleProgress"
    )
    Box(
        modifier = modifier
            .height(height)
            .background(trackColor, RoundedCornerShape(D.radiusPill))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(animated)
                .background(fillColor, RoundedCornerShape(D.radiusPill))
        )
    }
}

// ───────────────────────────────────────────────────────────────
// 6. 段染色胶囊：每段一种颜色（按餐次贡献），点击触发回调
// ───────────────────────────────────────────────────────────────

@Composable
fun LCSegmentedCapsule(
    segments: List<SegmentSpec>,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = D.progressTrackLg,
    onSegmentClick: ((index: Int) -> Unit)? = null
) {
    Box(
        modifier = modifier
            .height(height)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(D.radiusPill))
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            segments.forEachIndexed { i, seg ->
                val w = seg.fraction.coerceIn(0f, 1f)
                if (w > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .fillMaxWidth(w)
                            .background(seg.color, RoundedCornerShape(D.radiusPill))
                            .clickable(enabled = onSegmentClick != null) { onSegmentClick?.invoke(i) }
                    )
                }
            }
        }
    }
}

data class SegmentSpec(
    val color: Color,
    val fraction: Float
)

// ───────────────────────────────────────────────────────────────
// 7. 分类 emoji（共享：主食🍚 / 蛋白🍗 / 蔬果🥦 / 饮品🥛 / 其他🍽️）
// ───────────────────────────────────────────────────────────────

fun categoryEmojiOf(category: String): String = when (category) {
    "主食" -> "🍚"
    "蛋白" -> "🍗"
    "蔬果" -> "🥦"
    "饮品" -> "🥛"
    else   -> "🍽️"
}

// ───────────────────────────────────────────────────────────────
// 8. 同心圆环：用于首页"蛋白+蔬果+水分 3 环"，也可单环
//    实现：Canvas 画 stroke arc（不依赖第三方库）
// ───────────────────────────────────────────────────────────────

/** 圆环：track 灰 + fill 彩色。`progress ∈ 0..1`，size = 外径。stroke 在外径内侧。 */
@Composable
fun LCRing(
    progress: Float,
    color: Color,
    size: DpT = D.ringSize,
    strokeWidth: DpT = 10.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    content: @Composable () -> Unit = {}
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(Motion.LONG, easing = Motion.Emphasized),
        label = "ring"
    )
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val s = Size(this.size.width - stroke, this.size.height - stroke)
            val o = Offset(stroke / 2f, stroke / 2f)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = o,
                size = s,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                topLeft = o,
                size = s,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        content()
    }
}

/** 多层同心环：内到外画 N 个环，每环独立 progress + color。 */
@Composable
fun LCConcentricRings(
    rings: List<RingSpec>,
    size: DpT = 220.dp,
    ringGap: DpT = 14.dp,
    strokeWidth: DpT = 14.dp
) {
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val n = rings.size.coerceAtLeast(1)
            val stroke = strokeWidth.toPx()
            val gap = ringGap.toPx()
            val total = stroke * n + gap * (n - 1)
            rings.forEachIndexed { i, ring ->
                val inset = total / 2f - (stroke / 2f + i * (stroke + gap))
                val s = Size(this.size.width - 2 * inset, this.size.height - 2 * inset)
                val o = Offset(inset, inset)
                drawArc(
                    color = ring.trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = o,
                    size = s,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                drawArc(
                    color = ring.color,
                    startAngle = -90f,
                    sweepAngle = 360f * ring.progress.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = o,
                    size = s,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
    }
}

data class RingSpec(
    val color: Color,
    val progress: Float,
    val trackColor: Color = Color(0x14000000)
)

// ───────────────────────────────────────────────────────────────
// 9. 大图卡片入口（emoji + 圆形背景）
// ───────────────────────────────────────────────────────────────

@Composable
fun LCEmojiBadge(
    emoji: String,
    modifier: Modifier = Modifier,
    size: DpT = D.avatarLg,
    background: Color = PrimaryContainer,
    onClick: (() -> Unit)? = null
) {
    val base = modifier
        .size(size)
        .background(background, CircleShape)
    val final = if (onClick != null) base.clickable(onClick = onClick) else base
    Box(modifier = final, contentAlignment = Alignment.Center) {
        Text(emoji, style = MaterialTheme.typography.titleLarge)
    }
}

// ───────────────────────────────────────────────────────────────
// 10. 软渐变背景（用于顶部 hero / 状态卡） —— 暖奶油→浅柠檬
// ───────────────────────────────────────────────────────────────

fun lcWarmGradient(): Brush = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFFFFDE7),
        Color(0xFFFAFBC9)
    )
)

// ───────────────────────────────────────────────────────────────
// 11. 卡片标题（用于 ambientCard 内的"小标签"标题）
// ───────────────────────────────────────────────────────────────

@Composable
fun LCCardLabel(
    text: String,
    emoji: String? = null,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (emoji != null) {
            Text(emoji, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.width(S.xs))
        }
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = Outline,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ───────────────────────────────────────────────────────────────
// 12. AnimatedVisibility 的标准进入（淡入 + 缩放）
// ───────────────────────────────────────────────────────────────

@Composable
fun LCAppear(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(Motion.MEDIUM)) + scaleIn(tween(Motion.MEDIUM), initialScale = 0.96f),
        exit = fadeOut(tween(Motion.SHORT)) + scaleOut(tween(Motion.SHORT), targetScale = 0.98f),
        content = { content() }
    )
}
