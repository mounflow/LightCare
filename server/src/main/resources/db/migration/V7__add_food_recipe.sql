-- V7__add_food_recipe.sql
-- 食物做法（PR-Recipe）。
--
-- 设计：1:1 with lc_food_item（一个 food 最多一份 recipe，PR1 用户手填或 AI 生成都复用同一张表）。
-- 食材 / 调料 / 步骤都用 JSON 数组（Postgres 12+ 原生 JSONB 支持）。不用关联表是为了：
--   1) 一次 SELECT 拿全做法详情（避免 3 表 JOIN）
--   2) 食材 / 步骤顺序天然就是数组，JSONB 表达更直接
--   3) 字段可空，UI 按需渲染
--
-- Hibernate 6.5 驼峰 → snake_case 命名策略对"驼峰后接单字母"字段会推导错（如 cookingMinutes → cookingminutes 而非 cooking_minutes）。
-- 所以本表所有驼峰字段都用 snake_case 列名 + 应用层 @Column(name=...) 显式配对。

CREATE TABLE IF NOT EXISTS lc_food_recipe (
    food_id          BIGINT PRIMARY KEY REFERENCES lc_food_item(id) ON DELETE CASCADE,
    cooking_minutes  INTEGER NOT NULL DEFAULT 0,                 -- 烹饪时间（分钟），0 = 未填
    difficulty       VARCHAR(16) NOT NULL DEFAULT 'EASY',         -- EASY | MEDIUM | HARD
    ingredients_json JSONB NOT NULL DEFAULT '[]'::jsonb,         -- 食材列表：[{"name":"鸡蛋","amount":"2 个"}, ...]
    seasonings_json  JSONB NOT NULL DEFAULT '[]'::jsonb,         -- 调料列表：[{"name":"生抽","amount":"1 勺"}, ...]
    steps_json       JSONB NOT NULL DEFAULT '[]'::jsonb,         -- 步骤列表：[{"order":1,"text":"鸡蛋打散..."}, ...]
    source           VARCHAR(16) NOT NULL DEFAULT 'MANUAL',      -- MANUAL=用户手填, AI=拍照识别时 M3 顺便生成
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_food_recipe_source ON lc_food_recipe(source);
