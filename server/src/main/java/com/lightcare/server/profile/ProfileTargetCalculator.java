package com.lightcare.server.profile;

import java.time.LocalDate;

/**
 * 营养目标值计算器（Mifflin-St Jeor + PRD §1.4）。
 * 抽出来给 ProfileController（建档 / 更新 physique）和 AuthService（注册时建默认 profile）共用。
 *
 * 算法见 ProfileController.applyPhysiqueBasedTargets（这里只搬逻辑，不改语义）。
 *
 * 不再写"温和默认"假数据 —— 如果身高/体重缺失，target 字段保持 null。
 * 前端在 null 时显示"—" + 引导用户去"我的身体数据"补全。
 */
public final class ProfileTargetCalculator {

    private ProfileTargetCalculator() {}

    public static void apply(ProfileEntity p) {
        Integer h = p.getHeightCm();
        Double w = p.getWeightKg();
        if (h == null || w == null || h <= 0 || w <= 0) {
            // 不写假数据 —— 留 null，让前端提示用户去补全。
            return;
        }

        int age = (p.getBirthDate() != null)
            ? LocalDate.now().getYear() - p.getBirthDate().getYear()
            : 30;
        age = Math.max(age, 5);

        double bmr;
        String g = p.getGender() == null ? "U" : p.getGender().toUpperCase();
        switch (g) {
            case "M" -> bmr = 10 * w + 6.25 * h - 5 * age + 5;
            case "F" -> bmr = 10 * w + 6.25 * h - 5 * age - 161;
            default  -> bmr = 10 * w + 6.25 * h - 5 * age - 78;
        }

        double tdee = bmr * p.getActivityLevel().factor;
        int targetKcal = (int) Math.max(1500, tdee * 0.90);
        int proteinG  = (int) Math.round(w * 1.6);
        int waterMl   = (int) Math.round(w * 35);
        int steps     = switch (p.getActivityLevel()) {
            case SEDENTARY -> 6000;
            case LIGHT      -> 8000;
            case MODERATE   -> 10000;
            case ACTIVE     -> 12000;
            case VERY_ACTIVE -> 14000;
        };
        int vegServings = (age <= 18) ? 5 : (age <= 50 ? 5 : 4);

        p.setCalorieTargetKcal(targetKcal);
        p.setProteinTargetG(proteinG);
        p.setWaterTargetMl(waterMl);
        p.setStepTarget(steps);
        p.setVegTargetServings(vegServings);
    }
}