-- PR-Auth 注册时不再用 fallback 默认值（之前会给"温和成人"默认值导致假数据）。
-- 改为可空，让前端在 null 时显示"—" + 引导用户去"我的身体数据"补全身高/体重/年龄。
-- 蔬果目标（veg_target_servings）保留非空 + 默认 5（这是饮食建议，不是营养目标）。
ALTER TABLE lc_profile ALTER COLUMN protein_target_g DROP NOT NULL;
ALTER TABLE lc_profile ALTER COLUMN water_target_ml DROP NOT NULL;
ALTER TABLE lc_profile ALTER COLUMN step_target DROP NOT NULL;
ALTER TABLE lc_profile ALTER COLUMN calorie_target_kcal DROP NOT NULL;
-- 清空历史假数据（DB 已 TRUNCATE 过，这里再次兜底）
UPDATE lc_profile SET
    protein_target_g = NULL,
    water_target_ml = NULL,
    step_target = NULL,
    calorie_target_kcal = NULL;
