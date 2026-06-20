-- V1__init.sql
-- 轻养 LightCare 初始 schema

CREATE TABLE IF NOT EXISTS lc_user (
    id              BIGSERIAL PRIMARY KEY,
    phone           VARCHAR(32) UNIQUE,
    wechat_open_id  VARCHAR(64) UNIQUE,
    nickname        VARCHAR(64) NOT NULL,
    avatar_url      VARCHAR(512),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS lc_profile (
    id                   BIGSERIAL PRIMARY KEY,
    owner_user_id        BIGINT NOT NULL REFERENCES lc_user(id) ON DELETE CASCADE,
    managed_by_user_id   BIGINT REFERENCES lc_user(id) ON DELETE SET NULL,
    display_name         VARCHAR(64) NOT NULL,
    avatar_url           VARCHAR(512),
    relation             VARCHAR(16) NOT NULL DEFAULT 'SELF',
    birth_date           DATE,
    gender               VARCHAR(8),
    protein_target_g     INTEGER NOT NULL DEFAULT 60,
    veg_target_servings  INTEGER NOT NULL DEFAULT 5,
    water_target_ml      INTEGER NOT NULL DEFAULT 1700,
    step_target          INTEGER NOT NULL DEFAULT 8000,
    calorie_target_kcal  INTEGER NOT NULL DEFAULT 2000,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_profile_owner   ON lc_profile(owner_user_id);
CREATE INDEX IF NOT EXISTS idx_profile_manager ON lc_profile(managed_by_user_id);

CREATE TABLE IF NOT EXISTS lc_meal (
    id             BIGSERIAL PRIMARY KEY,
    profile_id     BIGINT NOT NULL REFERENCES lc_profile(id) ON DELETE CASCADE,
    slot           VARCHAR(16) NOT NULL,
    portion        VARCHAR(16) NOT NULL DEFAULT 'MEDIUM',
    source         VARCHAR(16) NOT NULL DEFAULT 'MANUAL',
    summary        VARCHAR(1024) NOT NULL,
    kcal           INTEGER NOT NULL DEFAULT 0,
    protein_g      DOUBLE PRECISION NOT NULL DEFAULT 0,
    fat_g          DOUBLE PRECISION NOT NULL DEFAULT 0,
    carb_g         DOUBLE PRECISION NOT NULL DEFAULT 0,
    fiber_g        DOUBLE PRECISION NOT NULL DEFAULT 0,
    veg_servings   INTEGER NOT NULL DEFAULT 0,
    meal_date      DATE NOT NULL,
    meal_time      TIME NOT NULL,
    logged_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_meal_profile_date ON lc_meal(profile_id, meal_date);
CREATE INDEX IF NOT EXISTS idx_meal_profile_time ON lc_meal(profile_id, logged_at);

CREATE TABLE IF NOT EXISTS lc_exercise (
    id             BIGSERIAL PRIMARY KEY,
    profile_id     BIGINT NOT NULL REFERENCES lc_profile(id) ON DELETE CASCADE,
    kind           VARCHAR(16) NOT NULL,
    intensity      VARCHAR(16) NOT NULL DEFAULT 'LIGHT',
    duration_min   INTEGER NOT NULL,
    steps          INTEGER NOT NULL DEFAULT 0,
    fatigue        INTEGER NOT NULL DEFAULT 1,
    exercise_date  DATE NOT NULL,
    logged_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_ex_profile_date ON lc_exercise(profile_id, exercise_date);

CREATE TABLE IF NOT EXISTS lc_water_log (
    id         BIGSERIAL PRIMARY KEY,
    profile_id BIGINT NOT NULL REFERENCES lc_profile(id) ON DELETE CASCADE,
    cups       INTEGER NOT NULL DEFAULT 1,
    log_date   DATE NOT NULL,
    logged_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_water_profile_date ON lc_water_log(profile_id, log_date);

CREATE TABLE IF NOT EXISTS lc_recommend_card (
    id              BIGSERIAL PRIMARY KEY,
    profile_id      BIGINT NOT NULL REFERENCES lc_profile(id) ON DELETE CASCADE,
    kind            VARCHAR(16) NOT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    title           VARCHAR(1024) NOT NULL,
    body            VARCHAR(1024) NOT NULL,
    candidates_json VARCHAR(2048),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    acted_at        TIMESTAMP WITH TIME ZONE
);
CREATE INDEX IF NOT EXISTS idx_recommend_profile ON lc_recommend_card(profile_id, created_at);
