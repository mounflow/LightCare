-- V6: 给 lc_meal 加 items_json 列（按食物分类染色用）
-- 旧数据为 NULL / 空串 → App 端染色兜底走 slot
ALTER TABLE lc_meal ADD COLUMN items_json TEXT NOT NULL DEFAULT '';
