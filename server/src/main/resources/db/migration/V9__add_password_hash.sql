-- 账号系统：lc_user 加 password_hash 列（bcrypt 哈希，~60 字符，128 足够）
-- phone 字段已存在 UNIQUE，注册时校验重名
-- 老 user（bootstrap 创建的占位账号）password_hash 为 NULL → 旧路径登录会被拒，需走"找回/重置"
--   此处先不写兜底，强制让老用户在新版本里走 register（手机号被占会失败，需要迁移策略见 plan）
ALTER TABLE lc_user
    ADD COLUMN IF NOT EXISTS password_hash VARCHAR(128);