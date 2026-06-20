package com.lightcare.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween

/**
 * 全 App 动效 token。
 *
 * - 短动效：125ms（按钮/选中态切换）
 * - 中动效：250ms（卡片入场、tab 切换）
 * - 长动效：400ms（页面级转场）
 * - emphasized: M3 emphasized 缓动，对应 cubic-bezier(0.2, 0, 0, 1)
 */
object Motion {
    const val SHORT = 125
    const val MEDIUM = 250
    const val LONG = 400

    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val Standard = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
    val Decelerate = CubicBezierEasing(0f, 0f, 0.2f, 1f)
    val Accelerate = CubicBezierEasing(0.4f, 0f, 1f, 1f)

    fun <T> shortTween() = tween<T>(SHORT, easing = Standard)
    fun <T> mediumTween() = tween<T>(MEDIUM, easing = Emphasized)
    fun <T> longTween() = tween<T>(LONG, easing = Emphasized)
}
