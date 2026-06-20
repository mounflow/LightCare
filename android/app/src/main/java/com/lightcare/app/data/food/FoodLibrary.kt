package com.lightcare.app.data.food

/**
 * 食物库（本地化：内置默认 + 用户自定义）。
 *
 * - [FoodItem]：食物条目，含蛋白/脂肪/碳水/蔬果份/热量/水分（每份）。
 * - [FoodLibrary.DEFAULTS]：内置 22 条常见中餐，不可删，可改。
 * - [FoodLibraryRepository]：合并"内置 + 用户自定义"，提供 search/all/add/remove。
 *
 * 营养字段口径与 MealCacheEntity/MealDto 对齐（protein/fat/carb/fiber/veg/kcal/waterMl），
 * 这样记录餐次时能完整累加，不再依赖 server 的经验公式估算。
 */
data class FoodItem(
    val key: String,
    val displayName: String,
    val category: String,            // 主食 / 蛋白 / 蔬果 / 饮品 / 其他
    val perServingProtein: Double,
    val perServingVeg: Int,
    val perServingKcal: Int,
    val perServingFat: Double = 0.0,
    val perServingCarb: Double = 0.0,
    val perServingWaterMl: Int = 0,  // PR1: 一份含水量（毫升）；米饭≈50，鸡胸≈65，苹果≈85，牛奶 250，汤 200
    val isDefault: Boolean = true,   // 内置=true 不可删；用户自定义=false
    val customId: Long? = null       // 自定义食物的 DB id（用于删除）；内置为 null
)

object FoodLibrary {

    /** 内置默认食物（不可删）。fat/carb/waterMl 按常识估值。 */
    val DEFAULTS: List<FoodItem> = listOf(
        FoodItem("rice", "米饭（一小碗）", "主食", 3.0, 0, 180, 0.5, 40.0, 50),
        FoodItem("noodles", "面条（一碗）", "主食", 5.0, 0, 280, 1.5, 55.0, 180),
        FoodItem("steamed_bun", "馒头（1 个）", "主食", 4.0, 0, 180, 1.0, 38.0, 30),
        FoodItem("congee", "白粥（一碗）", "主食", 1.5, 0, 120, 0.2, 26.0, 250),
        FoodItem("toast", "全麦吐司（2 片）", "主食", 5.0, 0, 160, 2.0, 28.0, 20),

        FoodItem("egg", "水煮蛋（1 个）", "蛋白", 6.5, 0, 78, 5.0, 0.6, 40),
        FoodItem("tofu", "豆腐（100 g）", "蛋白", 8.0, 0, 85, 4.8, 2.8, 80),
        FoodItem("chicken_breast", "鸡胸肉（100 g）", "蛋白", 23.0, 0, 165, 3.6, 0.0, 65),
        FoodItem("salmon", "三文鱼（100 g）", "蛋白", 20.0, 0, 208, 13.0, 0.0, 60),
        FoodItem("tuna_sandwich", "金枪鱼三明治（1 个）", "蛋白", 22.0, 1, 380, 14.0, 38.0, 30),
        FoodItem("yogurt", "无糖酸奶（1 杯）", "蛋白", 8.0, 0, 120, 3.0, 12.0, 180),
        FoodItem("milk", "牛奶（1 杯 250 ml）", "蛋白", 8.0, 0, 150, 8.0, 12.0, 250),

        FoodItem("broccoli", "清炒西兰花（一盘）", "蔬果", 4.0, 2, 90, 5.0, 7.0, 160),
        FoodItem("tomato_egg", "番茄炒蛋（一盘）", "蔬果", 12.0, 1, 180, 12.0, 6.0, 120),
        FoodItem("cucumber", "凉拌黄瓜（一盘）", "蔬果", 1.0, 1, 50, 3.0, 4.0, 140),
        FoodItem("lettuce", "生菜（一盘）", "蔬果", 1.0, 1, 25, 1.5, 3.0, 150),
        FoodItem("spinach", "清炒菠菜（一盘）", "蔬果", 3.0, 2, 70, 4.0, 5.0, 150),
        FoodItem("apple", "苹果（1 个）", "蔬果", 0.5, 1, 95, 0.3, 25.0, 140),
        FoodItem("banana", "香蕉（1 根）", "蔬果", 1.3, 0, 105, 0.4, 27.0, 100),

        FoodItem("tea", "茶（1 杯）", "饮品", 0.0, 0, 0, 0.0, 0.0, 250),
        FoodItem("coffee_black", "黑咖啡（1 杯）", "饮品", 0.0, 0, 5, 0.0, 1.0, 240),
        FoodItem("cola", "可乐（一罐）", "饮品", 0.0, 0, 140, 0.0, 39.0, 330)
    )

    /** 食量倍率：返回 protein/fat/carb/veg/kcal/water 六元组。 */
    fun multiply(item: FoodItem, portion: String): Multiplied {
        val mult = when (portion.uppercase()) {
            "SMALL" -> 0.7
            "LARGE" -> 1.4
            else -> 1.0
        }
        return Multiplied(
            protein = item.perServingProtein * mult,
            fat = item.perServingFat * mult,
            carb = item.perServingCarb * mult,
            veg = (item.perServingVeg * mult).toInt().coerceAtLeast(0),
            kcal = (item.perServingKcal * mult).toInt(),
            water = (item.perServingWaterMl * mult).toInt()
        )
    }
}

/** 一份食物按食量倍率后的营养。 */
data class Multiplied(
    val protein: Double,
    val fat: Double,
    val carb: Double,
    val veg: Int,
    val kcal: Int,
    val water: Int = 0
)