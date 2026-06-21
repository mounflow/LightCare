-- PR-Recipe: 推荐 meal 卡携带关联 foodId，客户端点推荐卡能跳详情看做法。
-- exercise 类卡此列为 NULL，不影响。
ALTER TABLE lc_recommend_card
    ADD COLUMN IF NOT EXISTS food_id BIGINT;