package com.lightcare.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 柠檬清爽风配色。
 *
 * 设计哲学：清爽、通透、像柠檬水。柠檬奶油底 + 嫩绿主色 + 柠檬绿正向点缀。
 * 整体色温统一在黄绿系，告别暖橙奶油的"浓甜"感，信息密度高的页面更耐看。
 *
 * - 柠檬奶油底 #FFFDE7：清透带暖，像柠檬水，长时间看不刺眼
 * - 嫩绿 #7CB342：主色，按钮/强调（比柠檬黄对比度够，白字看得清）
 * - 柠檬绿 #9CCC65：正向数据/达标/健康指标
 * - 浅柠檬 #F0F7C4：容器底（降饱和，不再"甜"，不抢戏）
 * - 深橄榄绿 #3E4A2F：文字主色（跟柠檬系协调，不用纯黑）
 *
 * 注：变量名沿用历史命名（Primary / Surface / BorderSubtle / SurgicalGreen 等），
 * 被全项目引用，只改色值不改名，做到无痛切换。
 */
// —— 主色：嫩绿 ——
val Primary = Color(0xFF7CB342)            // 嫩绿（按钮 / 标题 / 强调）
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFFF0F7C4)   // 浅柠檬容器（降饱和，清爽不抢戏）
val OnPrimaryContainer = Color(0xFF33691E)

// —— 柠檬绿（正向数据 / 达标 / 健康） ——
val SurgicalGreen = Color(0xFF9CCC65)      // 柠檬绿（沿用旧名，语义"正向/健康"）
val OnSurgicalGreen = Color(0xFF1B5E20)

// —— 文字主色：纯黑（清爽、对比强） ——
val ClinicalCharcoal = Color(0xFF000000)   // 沿用旧名，色值改纯黑

// —— 面 / 底 ——
val PureWhite = Color(0xFFFFFFFF)
val Background = Color(0xFFFFFDE7)         // 柠檬奶油底
val OnBackground = Color(0xFF000000)
val Surface = Color(0xFFFFFFFF)            // 卡片用纯白，浮在柠檬底上
val OnSurface = Color(0xFF000000)
val SurfaceBright = Color(0xFFFFFFFF)
val SurfaceContainerLowest = Color(0xFFFFFEF5)
val SurfaceContainerLow = Color(0xFFFFFCE0)
val SurfaceContainer = Color(0xFFF7F6D0)
val SurfaceContainerHigh = Color(0xFFEFF0C0)
val SurfaceContainerHighest = Color(0xFFE8E9B0)
val SurfaceVariant = Color(0xFFE8E9B0)
val OnSurfaceVariant = Color(0xFF1A1A1A)
val Outline = Color(0xFF1A1A1A)            // 次级文字（近黑）
val OutlineVariant = Color(0xFFC5CBA0)

// —— 描边（沿用旧名，色值改黄绿系） ——
val BorderSubtle = Color(0xFFE8EFD0)       // 默认浅黄绿描边
val BorderStrong = Color(0xFF7CB342)       // 激活/聚焦：嫩绿描边

// —— 错误（柔红，比冷红柔和） ——
val Error = Color(0xFFE5707A)
val OnError = Color(0xFFFFFFFF)
val ErrorContainer = Color(0xFFFFDAD6)
val OnErrorContainer = Color(0xFF93000A)

// —— 警告（琥珀黄，"接近目标但未超标"）——
// 注意 Secondary 也是 #9CCC65（和达标绿同色），接近目标不能用 Secondary，必须用这个独立琥珀色。
val Warning = Color(0xFFFFC107)

// —— 柠檬绿系（用于 Material secondary 占位 + 正向数据） ——
val Secondary = Color(0xFF9CCC65)
val OnSecondary = Color(0xFF1B5E20)
val SecondaryContainer = Color(0xFFE6F4C8)
val OnSecondaryContainer = Color(0xFF33691E)
val SecondaryFixed = Color(0xFFE6F4C8)
val SecondaryFixedDim = Color(0xFFD4E8A8)
val OnSecondaryFixed = Color(0xFF1B3A0A)
val OnSecondaryFixedVariant = Color(0xFF33691E)
val PrimaryFixed = Color(0xFFF0F7C4)
val PrimaryFixedDim = Color(0xFFE0EBA0)

// —— 营养三环：蛋白 / 蔬果 / 水分 ——
/** PRD 蛋白+蔬果+水分 3 环同心 —— 给色环专用。 */
val NutrientProtein = Color(0xFFE57373)   // 暖珊瑚红（蛋白）
val NutrientVeg = Color(0xFF7CB342)        // 嫩绿（蔬果，与主色同色相，分担正向语义）
val NutrientWater = Color(0xFF4FC3F7)      // 晴空蓝（水分）
val NutrientKcal = Color(0xFFFFB74D)       // 暖橙（能量）

/** 餐次色（早餐/午餐/晚餐/加餐），柔和粉彩避免冲突。 */
val SlotBreakfast = Color(0xFFFFB4A2)      // 蜜桃粉
val SlotLunch = Color(0xFFFFD68A)          // 麦黄
val SlotDinner = Color(0xFFB39DDB)         // 薰衣草
val SlotSnack = Color(0xFFFFAB91)          // 蜜橘

/** 成功 / 状态色（柔）。 */
val Success = Color(0xFF66BB6A)
val SuccessContainer = Color(0xFFC8E6C9)
