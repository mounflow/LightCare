package com.lightcare.app.ui.log

import java.time.LocalTime

enum class MealSlot(val display: String, val apiValue: String) {
    BREAKFAST("早餐", "BREAKFAST"),
    LUNCH("午餐", "LUNCH"),
    DINNER("晚餐", "DINNER"),
    SNACK("加餐", "SNACK");

    companion object {
        /** 根据当前时间智能推断（PRD §4.2.1） */
        fun fromCurrentTime(): MealSlot {
            val h = LocalTime.now().hour
            return when (h) {
                in 5..10 -> BREAKFAST
                in 11..14 -> LUNCH
                in 17..21 -> DINNER
                else -> SNACK
            }
        }
    }
}

enum class Portion(val display: String, val apiValue: String, val mult: Double) {
    SMALL("🍽 小份", "SMALL", 0.7),
    MEDIUM("适中", "MEDIUM", 1.0),
    LARGE("大份", "LARGE", 1.4)
}

enum class LogSource(val apiValue: String) {
    PHOTO("PHOTO"),
    VOICE("VOICE"),
    SEARCH("SEARCH"),
    MANUAL("MANUAL")
}

/** PR2: 编辑面板里的食材分类（与 FoodItem.category / M3 prompt 对齐）。 */
enum class FoodCategory(val display: String, val emoji: String) {
    STAPLE("主食", "🍚"),
    PROTEIN("蛋白", "🍗"),
    VEG("蔬果", "🥦"),
    DRINK("饮品", "🥛"),
    NUT("坚果", "🥜"),
    OTHER("其他", "🍽️");

    companion object {
        /** 把 server / FoodItem 里的字符串映射到枚举；没匹配上归"其他"。 */
        fun fromLabel(label: String?): FoodCategory = when (label?.trim()) {
            "主食" -> STAPLE
            "蛋白" -> PROTEIN
            "蔬果" -> VEG
            "饮品" -> DRINK
            "坚果" -> NUT
            else -> OTHER
        }
    }
}
