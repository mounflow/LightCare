package com.lightcare.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.lightcare.app.R

/**
 * 温暖圆润风排版（Quicksand）。
 *
 * Quicksand 是圆头收尾的几何 sans，亲和、可爱、不幼稚，长辈也能接受。
 * 已打进 res/font（medium / semibold / bold，无 regular，用 medium 兜 Normal）。
 *
 * 与 Clinical Precision 时代的差异：
 * - 字重偏粗一档（圆润字体细字重会发虚），层级靠字重 + 暖色而非全大写
 * - label 不再全大写 + 大字距（去掉技术蓝图感），改正常大小写 + 微字距
 * - 行高略松，呼吸感更好
 */
val Quicksand = FontFamily(
    Font(R.font.quicksand_medium, FontWeight.Normal),       // 用 medium 兜 Normal
    Font(R.font.quicksand_medium, FontWeight.Medium),
    Font(R.font.quicksand_semibold, FontWeight.SemiBold),
    Font(R.font.quicksand_bold, FontWeight.Bold),
)

val LightCareTypography = Typography(
    // —— Display：大标题，圆润可爱 ——
    displayLarge = TextStyle(
        fontFamily = Quicksand, fontWeight = FontWeight.Bold,
        fontSize = 44.sp, lineHeight = 52.sp, letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = Quicksand, fontWeight = FontWeight.Bold,
        fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = (-0.25).sp
    ),

    // —— Headline ——
    headlineLarge = TextStyle(
        fontFamily = Quicksand, fontWeight = FontWeight.Bold,
        fontSize = 30.sp, lineHeight = 38.sp, letterSpacing = (-0.25).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Quicksand, fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp, lineHeight = 34.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = Quicksand, fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp, lineHeight = 30.sp
    ),

    // —— Title ——
    titleLarge = TextStyle(
        fontFamily = Quicksand, fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp, lineHeight = 30.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Quicksand, fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp, lineHeight = 24.sp
    ),

    // —— Body ——
    bodyLarge = TextStyle(
        fontFamily = Quicksand, fontWeight = FontWeight.Medium,
        fontSize = 17.sp, lineHeight = 25.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Quicksand, fontWeight = FontWeight.Medium,
        fontSize = 15.sp, lineHeight = 22.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Quicksand, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, lineHeight = 18.sp
    ),

    // —— Label：正常大小写 + 微字距，不再全大写蓝图感 ——
    labelLarge = TextStyle(
        fontFamily = Quicksand, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Quicksand, fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.1.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Quicksand, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 16.sp
    )
)
