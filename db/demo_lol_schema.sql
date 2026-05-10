-- Demo：英雄联盟职业选手题库（数据来自 guessassin.xyz 公开 API，仅本地开发验证用）
-- 使用：psql -U <user> -d <database> -f db/demo_lol_schema.sql

DROP TABLE IF EXISTS demo_lol_player CASCADE;

CREATE TABLE demo_lol_player (
    id                    UUID PRIMARY KEY,
    game_id               VARCHAR(64)  NOT NULL,
    real_name             VARCHAR(128),
    age                   INTEGER,
    current_team          VARCHAR(128) NOT NULL,
    historical_teams      TEXT[]       NOT NULL DEFAULT '{}',
    region                VARCHAR(64)  NOT NULL,
    identity_regions      TEXT[]       NOT NULL DEFAULT '{}',
    positions             TEXT[]       NOT NULL DEFAULT '{}',
    birthplace            VARCHAR(64)  NOT NULL,
    champions             TEXT[]       NOT NULL DEFAULT '{}',
    status                VARCHAR(32)  NOT NULL,
    worlds_count          INTEGER      NOT NULL DEFAULT 0,
    championships_count   INTEGER      NOT NULL DEFAULT 0,
    avatar_url            TEXT,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now()
);

COMMENT ON TABLE demo_lol_player IS 'guessassin CN_KR 题库快照，用于猜人物 Demo 管线验证';

CREATE INDEX idx_demo_lol_game_id ON demo_lol_player (lower(game_id));
CREATE INDEX idx_demo_lol_real_name ON demo_lol_player (lower(real_name));
CREATE INDEX idx_demo_lol_positions ON demo_lol_player USING gin (positions);
CREATE INDEX idx_demo_lol_identity_regions ON demo_lol_player USING gin (identity_regions);
