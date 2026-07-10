-- 影视剧猜人物 — 通用表（attrs 存玩法字段；选项由 config/works/{work_id}/ 配置）
-- psql -d quiz_verse -f db/work_guess_schema.sql
-- psql -d quiz_verse -f db/seeds/zhenhuan_2011_seed.sql

BEGIN;

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE IF NOT EXISTS work (
    id              VARCHAR(64)  PRIMARY KEY,
    title_cn        VARCHAR(128) NOT NULL,
    category        VARCHAR(32)  NOT NULL DEFAULT 'drama',
    pool_type       VARCHAR(32)  NOT NULL DEFAULT 'single_work',
    schema_version  INT          NOT NULL DEFAULT 3,
    config_dir      VARCHAR(256),
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

COMMENT ON TABLE work IS '作品注册；config_dir 默认 config/works/{id}';
COMMENT ON COLUMN work.config_dir IS '本地字段/枚举配置目录，空则用 config/works/{id}';

CREATE TABLE IF NOT EXISTS work_column (
    work_id         VARCHAR(64)  NOT NULL REFERENCES work(id) ON DELETE CASCADE,
    column_key      VARCHAR(64)  NOT NULL,
    label_cn        VARCHAR(64)  NOT NULL,
    value_type      VARCHAR(32)  NOT NULL,
    compare_rule    VARCHAR(32)  NOT NULL,
    attrs_path      VARCHAR(128) NOT NULL,
    in_guess_table  BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order      INT          NOT NULL DEFAULT 0,
    description     TEXT,
    PRIMARY KEY (work_id, column_key)
);

COMMENT ON TABLE work_column IS '比对列元数据；选项列表以 config 下 json 为准，此处只管比对规则';

CREATE TABLE IF NOT EXISTS work_character (
    id              BIGSERIAL    PRIMARY KEY,
    work_id         VARCHAR(64)  NOT NULL REFERENCES work(id) ON DELETE CASCADE,
    display_name    VARCHAR(64)  NOT NULL,
    call_names      TEXT[]       NOT NULL DEFAULT '{}',
    attrs           JSONB        NOT NULL DEFAULT '{}',
    status          VARCHAR(16)  NOT NULL DEFAULT 'draft',
    sort_order      INT          NOT NULL DEFAULT 0,
    is_active       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_work_character_name UNIQUE (work_id, display_name),
    CONSTRAINT chk_work_character_status CHECK (status IN ('draft', 'ready'))
);

COMMENT ON TABLE work_character IS '角色；称呼在 call_names；其余玩法字段在 attrs';
COMMENT ON COLUMN work_character.call_names IS '称呼/绰号/小名，仅联想，不是位分';
COMMENT ON COLUMN work_character.attrs IS 'gender, role_type, titles[], residences[], major_plots[], connections[] 等';

CREATE INDEX IF NOT EXISTS idx_work_character_work ON work_character (work_id) WHERE is_active;
CREATE INDEX IF NOT EXISTS idx_work_character_display_name ON work_character (work_id, lower(display_name));
CREATE INDEX IF NOT EXISTS idx_work_character_call_names_gin ON work_character USING gin (call_names);
CREATE INDEX IF NOT EXISTS idx_work_character_attrs_gin ON work_character USING gin (attrs jsonb_path_ops);
CREATE INDEX IF NOT EXISTS idx_work_character_name_trgm ON work_character USING gin (display_name gin_trgm_ops);

-- 兼容旧列名 aliases
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'work_character' AND column_name = 'aliases'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'work_character' AND column_name = 'call_names'
    ) THEN
        ALTER TABLE work_character RENAME COLUMN aliases TO call_names;
    END IF;
END $$;

CREATE OR REPLACE FUNCTION work_character_attrs_is_object(p_attrs JSONB)
RETURNS BOOLEAN
LANGUAGE sql
IMMUTABLE
AS $$
    SELECT p_attrs IS NOT NULL
       AND jsonb_typeof(p_attrs) = 'object'
       AND p_attrs <> '{}'::jsonb;
$$;

ALTER TABLE work_character DROP CONSTRAINT IF EXISTS chk_work_character_attrs_object;
ALTER TABLE work_character DROP CONSTRAINT IF EXISTS chk_work_character_attrs_valid;
ALTER TABLE work_character DROP CONSTRAINT IF EXISTS chk_work_character_attrs_when_ready;

ALTER TABLE work_character
    ADD CONSTRAINT chk_work_character_attrs_when_ready
    CHECK (status = 'draft' OR work_character_attrs_is_object(attrs));

CREATE OR REPLACE FUNCTION work_character_connections_valid()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    cid BIGINT;
BEGIN
    IF NEW.attrs IS NULL OR NOT (NEW.attrs ? 'connections') THEN
        RETURN NEW;
    END IF;
    IF jsonb_typeof(NEW.attrs->'connections') <> 'array' THEN
        RAISE EXCEPTION 'connections 必须是 JSON 数组';
    END IF;
    FOR cid IN
        SELECT (e #>> '{}')::bigint
        FROM jsonb_array_elements(NEW.attrs->'connections') AS e
    LOOP
        IF NOT EXISTS (
            SELECT 1 FROM work_character wc
            WHERE wc.work_id = NEW.work_id AND wc.id = cid
        ) THEN
            RAISE EXCEPTION 'connections 含无效角色 id %', cid;
        END IF;
    END LOOP;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_work_character_connections ON work_character;
CREATE CONSTRAINT TRIGGER trg_work_character_connections
    AFTER INSERT OR UPDATE OF attrs ON work_character
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
    EXECUTE PROCEDURE work_character_connections_valid();

COMMIT;
