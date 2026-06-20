package com.lightcare.server.meal;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 营养估算占位。真实实现要接 ONNX 模型或第三方 API（PRD §7 W3）。
 * P4 提供"按食物 key 估"的最小可用版。
 *
 * 注意（PR-C 之后）：这里的 SEED 是老的手动记录路径（POST /v1/meals 传 foodKeys）用的估算占位，
 * 已被 V4 的 lc_food_item 表（22 条种子，营养完整）取代。本类仅向后兼容老路径保留，
 * 新功能（拍照识别、食物库 CRUD、推荐）都走 lc_food_item。后续手动记录也可迁到查 lc_food_item 时再废弃。
 */
@Service
public class AiEstimateService {

    public record FoodEstimate(String key, String displayName, double proteinG, int vegServings, int kcal) {}

    public record EstimateResult(int kcal, double proteinG, double fatG, double carbG, double fiberG, int vegServings) {}

    private static final List<FoodEstimate> SEED = List.of(
        new FoodEstimate("rice", "米饭", 3.0, 0, 180),
        new FoodEstimate("noodles", "面条", 5.0, 0, 280),
        new FoodEstimate("toast", "全麦吐司", 5.0, 0, 160),
        new FoodEstimate("egg", "水煮蛋", 6.5, 0, 78),
        new FoodEstimate("tofu", "豆腐", 8.0, 0, 85),
        new FoodEstimate("chicken_breast", "鸡胸肉", 23.0, 0, 165),
        new FoodEstimate("salmon", "三文鱼", 20.0, 0, 208),
        new FoodEstimate("tuna_sandwich", "金枪鱼三明治", 22.0, 1, 380),
        new FoodEstimate("broccoli", "清炒西兰花", 4.0, 2, 90),
        new FoodEstimate("tomato_egg", "番茄炒蛋", 12.0, 1, 180),
        new FoodEstimate("cucumber", "凉拌黄瓜", 1.0, 1, 50),
        new FoodEstimate("apple", "苹果", 0.5, 1, 95),
        new FoodEstimate("banana", "香蕉", 1.3, 0, 105),
        new FoodEstimate("milk", "牛奶", 8.0, 0, 150),
        new FoodEstimate("yogurt", "无糖酸奶", 8.0, 0, 120)
    );

    public EstimateResult estimate(List<String> keys, String portion) {
        double mult = switch (portion == null ? "MEDIUM" : portion) {
            case "SMALL" -> 0.7;
            case "LARGE" -> 1.4;
            default -> 1.0;
        };
        double protein = 0, fat = 0, carb = 0, fiber = 0;
        int kcal = 0, veg = 0;
        for (String key : keys) {
            var found = SEED.stream().filter(s -> s.key().equals(key)).findFirst();
            if (found.isEmpty()) continue;
            var s = found.get();
            protein += s.proteinG() * mult;
            veg += (int) Math.round(s.vegServings() * mult);
            kcal += (int) Math.round(s.kcal() * mult);
            fat += s.proteinG() * mult * 0.4;
            carb += s.kcal() * mult * 0.5 / 4.0;
            fiber += 2.0 * mult;
        }
        return new EstimateResult(kcal, protein, fat, carb, fiber, veg);
    }
}
