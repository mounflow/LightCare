package com.lightcare.server.report;

import com.lightcare.server.exercise.ExerciseRepository;
import com.lightcare.server.exercise.WaterLogEntity;
import com.lightcare.server.exercise.WaterLogRepository;
import com.lightcare.server.meal.MealRepository;
import com.lightcare.server.profile.ProfileEntity;
import com.lightcare.server.profile.ProfileRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

@Service
public class WeeklyReportService {

    private final MealRepository mealRepo;
    private final ExerciseRepository exerciseRepo;
    private final ProfileRepository profileRepo;
    private final WaterLogRepository waterRepo;

    public WeeklyReportService(MealRepository mealRepo, ExerciseRepository exerciseRepo,
                               ProfileRepository profileRepo, WaterLogRepository waterRepo) {
        this.mealRepo = mealRepo;
        this.exerciseRepo = exerciseRepo;
        this.profileRepo = profileRepo;
        this.waterRepo = waterRepo;
    }

    public record Nutrition(double proteinPct, int vegPct, int waterPct, int kcalPct) {}
    public record Highlight(String emoji, String title, String body) {}
    public record WeeklyReport(LocalDate weekStart, LocalDate weekEnd, int daysLogged,
                                int mealCount, Nutrition nutrition,
                                String praise, List<Highlight> highlights) {}

    public WeeklyReport build(long profileId) {
        ProfileEntity p = profileRepo.findById(profileId).orElseThrow();
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);

        // 7 天营养达成率
        double targetProtein = p.getProteinTargetG() * 7.0;
        double targetVeg = p.getVegTargetServings() * 7.0;
        double targetWater = p.getWaterTargetMl() * 7.0;
        double targetKcal = p.getCalorieTargetKcal() * 7.0;

        double proteinSum = 0;
        int vegSum = 0;
        int waterSum = 0;
        int kcalSum = 0;
        int daysLogged = 0;
        int mealCount = 0;
        for (int i = 0; i < 7; i++) {
            LocalDate d = weekStart.plusDays(i);
            double pr = mealRepo.sumProteinByDate(profileId, d);
            int vg = mealRepo.sumVegServingsByDate(profileId, d);
            // 水分 = 餐内水分 + 手动打卡（每杯按 200ml）
            int w = mealRepo.sumWaterMlByDate(profileId, d)
                + waterRepo.sumCupsByDate(profileId, d) * WaterLogEntity.CUP_ML;
            int kc = mealRepo.sumKcalByDate(profileId, d);
            proteinSum += pr;
            vegSum += vg;
            waterSum += w;
            kcalSum += kc;
            if (pr + vg + w + kc > 0) daysLogged++;
            mealCount += mealRepo.findByProfileIdAndMealDateOrderByMealTime(profileId, d).size();
        }

        int proteinPct = pct(proteinSum, targetProtein);
        int vegPct = pct(vegSum, targetVeg);
        int waterPct = pct(waterSum, targetWater);
        int kcalPct = pct(kcalSum, targetKcal);
        Nutrition nutrition = new Nutrition(proteinPct, vegPct, waterPct, kcalPct);

        String praise = buildPraise(p, daysLogged, mealCount, proteinPct);
        List<Highlight> highlights = buildHighlights(profileId, weekStart);

        return new WeeklyReport(weekStart, weekEnd, daysLogged, mealCount, nutrition, praise, highlights);
    }

    private int pct(double actual, double target) {
        if (target <= 0) return 0;
        return (int) Math.min(100, Math.round(actual * 100.0 / target));
    }

    private String buildPraise(ProfileEntity p, int daysLogged, int mealCount, int proteinPct) {
        if (daysLogged == 0) {
            return "下周我们一起重新开始 :)";
        }
        if (daysLogged >= 5) {
            return "你做得很棒！这周记录了 " + daysLogged + " 天，" + mealCount + " 顿饭。每一次小的进步都值得被看见。";
        }
        if (proteinPct >= 80) {
            return "蛋白质目标达成 " + proteinPct + "%，身体会更感谢你的。";
        }
        return "这周已经慢慢在节奏里了——再坚持一下，下周会更有感觉。";
    }

    private List<Highlight> buildHighlights(long profileId, LocalDate weekStart) {
        List<Highlight> list = new ArrayList<>();
        // 找"连续 3 天午餐吃了蔬菜"这样的事实
        boolean lunchWithVegStreak3 = false;
        int consecutive = 0;
        for (int i = 0; i < 7; i++) {
            LocalDate d = weekStart.plusDays(i);
            var meals = mealRepo.findByProfileIdAndMealDateOrderByMealTime(profileId, d);
            boolean hasLunchVeg = meals.stream().anyMatch(m -> "LUNCH".equals(m.getSlot().name()) && m.getVegServings() >= 1);
            if (hasLunchVeg) {
                consecutive++;
                if (consecutive >= 3) {
                    lunchWithVegStreak3 = true;
                    break;
                }
            } else {
                consecutive = 0;
            }
        }
        if (lunchWithVegStreak3) {
            list.add(new Highlight("🥗", "连续 3 天午餐都有蔬菜",
                "从周一到周五，你为身体多添了一点绿色。"));
        }
        // 总记录天数 ≥ 4 → 鼓励卡
        int loggedDays = 0;
        for (int i = 0; i < 7; i++) {
            LocalDate d = weekStart.plusDays(i);
            if (!mealRepo.findByProfileIdAndMealDateOrderByMealTime(profileId, d).isEmpty()) loggedDays++;
        }
        if (loggedDays >= 4) {
            list.add(new Highlight("☀", "更美好的早餐习惯",
                "你 " + loggedDays + " 天的记录，节奏正在被悄悄建立。"));
        }
        if (list.isEmpty()) {
            list.add(new Highlight("🌱", "一切刚刚开始",
                "愿意记录，已经是迈出的一大步。"));
        }
        return list;
    }
}
