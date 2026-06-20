-- V3: 水分字段 + 异步识别状态字段（拍照即入列 PR1）
-- water_ml: meal 包含的液体毫升（汤/粥/饮料 + 食材本身含水）
-- recognition_status: DONE=已识别完（含手动/食物库历史）, PENDING=等待异步识别, FAILED=识别失败
ALTER TABLE lc_meal ADD COLUMN water_ml INTEGER NOT NULL DEFAULT 0;
ALTER TABLE lc_meal ADD COLUMN recognition_status VARCHAR(16) NOT NULL DEFAULT 'DONE';