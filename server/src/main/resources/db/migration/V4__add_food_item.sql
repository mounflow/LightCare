-- V4__add_food_item.sql
-- 食物库统一到 server 管理（PR-C）。
--
-- 作用域：user 级。owner_user_id = 用户自己的食物；owner_user_id = NULL = 全局种子（所有用户只读共享）。
-- 种子数据来自原 Android 端 FoodLibrary.DEFAULTS（22 条），迁上来做全局种子。
-- 拍照识别（MealRecognitionExecutor.autoUpsertFoods）识别完的新食物 source=AI 写入。
-- 重名判定（四项营养 kcal/蛋白/脂肪/碳水 ±5% 全等）：由应用层 FoodController/MealRecognitionExecutor 负责，
-- conflict_status=PENDING_CONFLICT 表示"同名不同营养"，等用户决定换名/覆盖/跳过。

CREATE TABLE IF NOT EXISTS lc_food_item (
    id              BIGSERIAL PRIMARY KEY,
    owner_user_id   BIGINT REFERENCES lc_user(id) ON DELETE CASCADE,  -- NULL = 全局种子
    key             VARCHAR(64)  NOT NULL,                  -- 业务 key（"rice" / "custom_<ts>" / "ai_<ts>_<hash>"）
    display_name    VARCHAR(128) NOT NULL,                  -- 带"份量"后缀，如 "米饭（一小碗）" / "米饭(180g)"
    category        VARCHAR(32)  NOT NULL DEFAULT '其他',   -- 主食|蛋白|蔬果|饮品|坚果|其他
    source          VARCHAR(16)  NOT NULL DEFAULT 'MANUAL', -- MANUAL=手加, AI=拍照识别自动入库, SEED=种子迁移
    per_serving_g   INTEGER      NOT NULL DEFAULT 0,        -- 1 份的克数（AI 入库时 = M3 的 weightG）
    kcal            INTEGER      NOT NULL DEFAULT 0,
    protein_g       DOUBLE PRECISION NOT NULL DEFAULT 0,
    fat_g           DOUBLE PRECISION NOT NULL DEFAULT 0,
    carb_g          DOUBLE PRECISION NOT NULL DEFAULT 0,
    fiber_g         DOUBLE PRECISION NOT NULL DEFAULT 0,
    water_ml        INTEGER      NOT NULL DEFAULT 0,
    veg_servings    INTEGER      NOT NULL DEFAULT 0,
    is_default      BOOLEAN      NOT NULL DEFAULT FALSE,    -- TRUE = 内置种子（前端标识"不可删"）
    conflict_status VARCHAR(16)  NOT NULL DEFAULT 'RESOLVED', -- RESOLVED | PENDING_CONFLICT
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_food_owner_name   ON lc_food_item(owner_user_id, display_name);
CREATE INDEX IF NOT EXISTS idx_food_owner_source ON lc_food_item(owner_user_id, source);
CREATE INDEX IF NOT EXISTS idx_food_conflict     ON lc_food_item(owner_user_id, conflict_status);

-- 种子：原 Android FoodLibrary.DEFAULTS 22 条（owner_user_id=NULL, source=SEED, is_default=TRUE）。
-- per_serving_g 按"一小碗/1 个/1 杯"语境取整。
-- 字段顺序：key, display_name, category, per_serving_g, kcal, protein_g, fat_g, carb_g, fiber_g, water_ml, veg_servings
INSERT INTO lc_food_item
    (owner_user_id, key, display_name, category, source, per_serving_g, kcal, protein_g, fat_g, carb_g, fiber_g, water_ml, veg_servings, is_default)
VALUES
    -- 主食
    (NULL, 'rice',           '米饭（一小碗）',     '主食', 'SEED', 180, 180, 3.0,  0.5, 40.0, 1.5, 50,  0, TRUE),
    (NULL, 'noodles',        '面条（一碗）',       '主食', 'SEED', 250, 280, 5.0,  1.5, 55.0, 2.0, 180, 0, TRUE),
    (NULL, 'steamed_bun',    '馒头（1 个）',       '主食', 'SEED', 100, 180, 4.0,  1.0, 38.0, 1.5, 30,  0, TRUE),
    (NULL, 'congee',         '白粥（一碗）',       '主食', 'SEED', 300, 120, 1.5,  0.2, 26.0, 0.5, 250, 0, TRUE),
    (NULL, 'toast',          '全麦吐司（2 片）',   '主食', 'SEED', 60,  160, 5.0,  2.0, 28.0, 3.0, 20,  0, TRUE),
    -- 蛋白
    (NULL, 'egg',            '水煮蛋（1 个）',     '蛋白', 'SEED', 50,  78,  6.5,  5.0, 0.6,  0.0, 40,  0, TRUE),
    (NULL, 'tofu',           '豆腐（100 g）',      '蛋白', 'SEED', 100, 85,  8.0,  4.8, 2.8,  0.5, 80,  0, TRUE),
    (NULL, 'chicken_breast', '鸡胸肉（100 g）',    '蛋白', 'SEED', 100, 165, 23.0, 3.6, 0.0,  0.0, 65,  0, TRUE),
    (NULL, 'salmon',         '三文鱼（100 g）',    '蛋白', 'SEED', 100, 208, 20.0, 13.0,0.0,  0.0, 60,  0, TRUE),
    (NULL, 'tuna_sandwich',  '金枪鱼三明治（1 个）','蛋白','SEED', 150, 380, 22.0, 14.0,38.0, 2.0, 30,  1, TRUE),
    (NULL, 'yogurt',         '无糖酸奶（1 杯）',   '蛋白', 'SEED', 150, 120, 8.0,  3.0, 12.0, 0.0, 180, 0, TRUE),
    (NULL, 'milk',           '牛奶（1 杯 250 ml）','蛋白', 'SEED', 250, 150, 8.0,  8.0, 12.0, 0.0, 250, 0, TRUE),
    -- 蔬果
    (NULL, 'broccoli',       '清炒西兰花（一盘）', '蔬果', 'SEED', 150, 90,  4.0,  5.0, 7.0,  3.0, 160, 2, TRUE),
    (NULL, 'tomato_egg',     '番茄炒蛋（一盘）',   '蔬果', 'SEED', 200, 180, 12.0, 12.0,6.0,  2.0, 120, 1, TRUE),
    (NULL, 'cucumber',       '凉拌黄瓜（一盘）',   '蔬果', 'SEED', 150, 50,  1.0,  3.0, 4.0,  1.5, 140, 1, TRUE),
    (NULL, 'lettuce',        '生菜（一盘）',       '蔬果', 'SEED', 100, 25,  1.0,  1.5, 3.0,  1.5, 150, 1, TRUE),
    (NULL, 'spinach',        '清炒菠菜（一盘）',   '蔬果', 'SEED', 150, 70,  3.0,  4.0, 5.0,  2.5, 150, 2, TRUE),
    (NULL, 'apple',          '苹果（1 个）',       '蔬果', 'SEED', 200, 95,  0.5,  0.3, 25.0, 3.0, 140, 1, TRUE),
    (NULL, 'banana',         '香蕉（1 根）',       '蔬果', 'SEED', 120, 105, 1.3,  0.4, 27.0, 2.0, 100, 0, TRUE),
    -- 饮品
    (NULL, 'tea',            '茶（1 杯）',         '饮品', 'SEED', 250, 0,   0.0,  0.0, 0.0,  0.0, 250, 0, TRUE),
    (NULL, 'coffee_black',   '黑咖啡（1 杯）',     '饮品', 'SEED', 240, 5,   0.0,  0.0, 1.0,  0.0, 240, 0, TRUE),
    (NULL, 'cola',           '可乐（一罐）',       '饮品', 'SEED', 330, 140, 0.0,  0.0, 39.0, 0.0, 330, 0, TRUE);
