package com.lightcare.server.recommend;

import com.lightcare.server.exercise.ExerciseRepository;
import com.lightcare.server.food.FoodRepository;
import com.lightcare.server.meal.MealRepository;
import com.lightcare.server.profile.ProfileEntity;
import com.lightcare.server.profile.ProfileRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/**
 * 推荐规则引擎（PRD §5.1 饮食 + §5.2 运动）。
 * 输入：profileId + 用户自报疲劳度（运动场景）。
 * 输出：1-2 张推荐卡（食 + 动）。
 *
 * PR-Recipe: meal 类 Recommendation 携带一个关联 foodId（从食物库按关键字匹配），
 * 让客户端点推荐卡能跳详情页看做法。
 */
@Service
public class RecommendEngine {

    private final MealRepository mealRepo;
    private final ExerciseRepository exerciseRepo;
    private final ProfileRepository profileRepo;
    private final FoodRepository foodRepo;

    /** PR-Recipe: 每 intent 对应的种子食物关键字（按顺序匹配，第一个存在的当候选）。
     *  没匹配上 → foodId 为 null，客户端不进详情页（仍显示卡片）。 */
    private static final Map<String, List<String>> MEAL_KEYWORDS = Map.of(
        "balanced_combo", List.of("三明治", "吐司"),
        "protein_focus",  List.of("豆腐", "鸡胸", "三文鱼", "蛋"),
        "veg_focus",      List.of("西兰花", "菠菜", "番茄", "生菜", "黄瓜"),
        "hydration",      List.of("茶", "牛奶")
    );

    public RecommendEngine(MealRepository mealRepo, ExerciseRepository exerciseRepo,
                           ProfileRepository profileRepo, FoodRepository foodRepo) {
        this.mealRepo = mealRepo;
        this.exerciseRepo = exerciseRepo;
        this.profileRepo = profileRepo;
        this.foodRepo = foodRepo;
    }

    public record NutrientGap(double proteinG, int vegServings, int waterMl) {}
    public record DailyAggregate(int kcal, double proteinG, int vegServings, int waterCups, int stepsToday) {}
    /** PR-Recipe: meal 类卡携带 foodId（nullable）；exercise 类为 null。 */
    public record Recommendation(Kind kind, String intent, String title, String body, Long foodId) {}

    public enum Kind { MEAL, EXERCISE }

    public List<Recommendation> recommendMeal(long profileId) {
        NutrientGap gap = calcThreeDayGap(profileId);
        String intent;
        String title, body;
        if (gap.proteinG() >= 15 && gap.vegServings() >= 2) {
            intent = "balanced_combo";
            title = "蛋白质和蔬菜都还差点～";
            body = "午餐加个金枪鱼三明治怎么样？清爽又助你轻松达标。";
        } else if (gap.proteinG() >= 15) {
            intent = "protein_focus";
            title = "今天蛋白质摄入有点少哦";
            body = "晚餐来份豆腐蒸虾仁？简单又补蛋白。";
        } else if (gap.vegServings() >= 2) {
            intent = "veg_focus";
            title = "蔬菜好像还没吃够呢";
            body = "晚饭加一盘清炒西兰花？五颜六色心情也好。";
        } else if (gap.waterMl() >= 400) {
            intent = "hydration";
            title = "今天喝的水不太够";
            body = "下午泡杯花茶，暖暖的也补水。";
        } else {
            return List.of();
        }
        Long foodId = pickFoodId(intent);
        return List.of(new Recommendation(Kind.MEAL, intent, title, body, foodId));
    }

    public List<Recommendation> recommendExercise(long profileId, int fatigueInput, int stepsTodayOverride) {
        if (fatigueInput >= 3) {
            return List.of(new Recommendation(
                Kind.EXERCISE, "rest",
                "今天好好休息吧",
                "辛苦啦，一杯温水 + 早点睡，明天再动起来。",
                null));
        }
        DailyAggregate agg = aggregateToday(profileId, stepsTodayOverride);
        int surplus = Math.max(0, agg.kcal() - kcalTargetFor(profileId));
        int steps = agg.stepsToday();

        int duration;
        String type, intensity;
        if (surplus >= 200 && steps < 4000 && fatigueInput == 0) {
            type = "慢跑"; duration = 30; intensity = "中-高";
        } else if (surplus >= 200 && fatigueInput <= 1) {
            type = "快走"; duration = 20; intensity = "中";
        } else if (fatigueInput == 2 && surplus < 200) {
            type = "拉伸"; duration = 5; intensity = "极轻";
        } else if (steps < 6000) {
            type = "散步"; duration = 15; intensity = "轻";
        } else {
            return List.of();
        }
        return List.of(new Recommendation(
            Kind.EXERCISE, "exercise",
            "傍晚" + type + "？",
            duration + " 分钟的" + type + "可以提升你的心情哦。",
            null));
    }

    /** PR-Recipe: 按 intent 关键字从食物库（用户可见范围）里挑一个真实 foodId。
     *  没匹配上 → null（卡片仍显示，但点不动）。 */
    private Long pickFoodId(String intent) {
        var keys = MEAL_KEYWORDS.get(intent);
        if (keys == null) return null;
        // 用户的 ownerUserId 通过 profileId 间接推不到（profileId != userId），用 NULL → 走种子可见范围。
        // 这样不挑到其他用户的私有食物，且保证 22 条 seed 一定能匹配上。
        for (String kw : keys) {
            var matches = foodRepo.findVisible(null).stream()
                .filter(f -> f.getDisplayName() != null && f.getDisplayName().contains(kw))
                .toList();
            if (!matches.isEmpty()) return matches.get(0).getId();
        }
        return null;
    }

    /** 过去 3 天缺口（蛋白 / 蔬果 / 水分） */
    public NutrientGap calcThreeDayGap(long profileId) {
        ProfileEntity p = profileRepo.findById(profileId).orElse(null);
        LocalDate today = LocalDate.now();
        double protein = 0;
        int veg = 0;
        int waterMl = 0;
        for (int i = 0; i < 3; i++) {
            LocalDate d = today.minusDays(i);
            protein += mealRepo.sumProteinByDate(profileId, d);
            veg += mealRepo.sumVegServingsByDate(profileId, d);
            // 水分：暂按 0 计；P5 收尾接 lc_water_log
        }
        if (p == null) return new NutrientGap(0, 0, 0);
        double gapProtein = Math.max(0, p.getProteinTargetG() * 3 - protein);
        int gapVeg = Math.max(0, p.getVegTargetServings() * 3 - veg);
        int gapWater = Math.max(0, p.getWaterTargetMl() * 3 / 1000 * 1000 - waterMl);
        return new NutrientGap(gapProtein, gapVeg, gapWater);
    }

    public DailyAggregate aggregateToday(long profileId, int stepsTodayOverride) {
        LocalDate today = LocalDate.now();
        int kcal = mealRepo.sumKcalByDate(profileId, today);
        double protein = mealRepo.sumProteinByDate(profileId, today);
        int veg = mealRepo.sumVegServingsByDate(profileId, today);
        int waterCups = 0; // P5 接
        int steps = stepsTodayOverride;
        return new DailyAggregate(kcal, protein, veg, waterCups, steps);
    }

    private int kcalTargetFor(long profileId) {
        return profileRepo.findById(profileId).map(ProfileEntity::getCalorieTargetKcal).orElse(2000);
    }
}
