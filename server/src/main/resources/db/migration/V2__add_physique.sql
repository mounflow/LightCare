-- V2: 生理数据字段（方案 3）
-- 身高/体重可空，未采集时按 V1 默认值（PRD §1.4 三档）
-- activity_level 默认 LIGHT，活动量系数 1.375
ALTER TABLE lc_profile ADD COLUMN height_cm INTEGER;
ALTER TABLE lc_profile ADD COLUMN weight_kg DOUBLE PRECISION;
ALTER TABLE lc_profile ADD COLUMN activity_level VARCHAR(16) DEFAULT 'LIGHT';