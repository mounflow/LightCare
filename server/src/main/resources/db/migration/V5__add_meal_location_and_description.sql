-- V5__add_meal_location_and_description.sql
-- 给 meal 加两个字段（详情页信息流卡片用）：
--   location     地点名（反地理编码得到，如「北京市海淀区」/「妈妈家」），可空
--   description  关于这次美食的描述/笔记（用户手填的心情），可空；与 summary（自动拼的食物名）分开
ALTER TABLE lc_meal ADD COLUMN location VARCHAR(128);
ALTER TABLE lc_meal ADD COLUMN description VARCHAR(512);
