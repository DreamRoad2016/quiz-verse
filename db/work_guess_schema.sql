-- 影视剧 / 单作品猜人物 — 通用表结构（所有作品共用）
-- 一局锁一个 work_id；列定义在 work_column；角色在 work_character.attrs (jsonb)
-- 使用：psql -d quiz_verse -f db/work_guess_schema.sql
-- 某部作品元数据：psql -d quiz_verse -f db/seeds/zhenhuan_2011_seed.sql

BEGIN;

CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ---------------------------------------------------------------------------
-- work：作品/题库边界（甄嬛传、三体… 各一行，不是各一张表）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS work (
    id              VARCHAR(64)  PRIMARY KEY,          -- 业务 slug，如 zhenhuan_2011、santi_2023
    title_cn        VARCHAR(128) NOT NULL,
    category        VARCHAR(32)  NOT NULL DEFAULT 'drama',
    pool_type       VARCHAR(32)  NOT NULL DEFAULT 'single_work',
    schema_version  INT          NOT NULL DEFAULT 1,
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

COMMENT ON TABLE work IS '作品注册表；影视剧一局只猜同一 work_id 下的角色';
COMMENT ON COLUMN work.id IS '作品唯一标识（非表名）；新剧 INSERT 新行即可';

-- ---------------------------------------------------------------------------
-- work_column：每部作品自己的比对列（列 key、规则、枚举说明）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS work_column (
    work_id         VARCHAR(64)  NOT NULL REFERENCES work(id) ON DELETE CASCADE,
    column_key      VARCHAR(64)  NOT NULL,
    label_cn        VARCHAR(64)  NOT NULL,
    value_type      VARCHAR(32)  NOT NULL,
    compare_rule    VARCHAR(32)  NOT NULL,
    attrs_path      VARCHAR(128) NOT NULL,
    in_guess_table  BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order      INT          NOT NULL DEFAULT 0,
    enum_values     JSONB,
    description     TEXT,
    PRIMARY KEY (work_id, column_key)
);

COMMENT ON TABLE work_column IS '列元数据；不同 work_id 可有不同列集合与 enum_values';

-- ---------------------------------------------------------------------------
-- work_character：角色（全作品共用一张表，用 work_id 区分）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS work_character (
    id              BIGSERIAL    PRIMARY KEY,
    work_id         VARCHAR(64)  NOT NULL REFERENCES work(id) ON DELETE CASCADE,
    display_name    VARCHAR(64)  NOT NULL,
    aliases         TEXT[]       NOT NULL DEFAULT '{}',
    attrs           JSONB        NOT NULL DEFAULT '{}',
    sort_order      INT          NOT NULL DEFAULT 0,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_work_character_name UNIQUE (work_id, display_name)
);

COMMENT ON TABLE work_character IS '全作品角色；attrs 键名须与本作品 work_column.attrs_path 一致';
COMMENT ON COLUMN work_character.aliases IS '别称，仅联想，不参与比对';
COMMENT ON COLUMN work_character.attrs IS '比对属性 JSON；合法值见该 work 的 work_column.enum_values';

CREATE INDEX IF NOT EXISTS idx_work_character_work ON work_character (work_id) WHERE is_active;
CREATE INDEX IF NOT EXISTS idx_work_character_display_name ON work_character (work_id, lower(display_name));
CREATE INDEX IF NOT EXISTS idx_work_character_aliases_gin ON work_character USING gin (aliases);
CREATE INDEX IF NOT EXISTS idx_work_character_attrs_gin ON work_character USING gin (attrs jsonb_path_ops);
CREATE INDEX IF NOT EXISTS idx_work_character_name_trgm ON work_character USING gin (display_name gin_trgm_ops);

-- attrs 仅做结构级校验；枚举/必填列由 work_column + 应用层/导入脚本保证
CREATE OR REPLACE FUNCTION work_character_attrs_is_object(p_attrs JSONB)
RETURNS BOOLEAN
LANGUAGE sql
IMMUTABLE
AS $$
    SELECT p_attrs IS NOT NULL
       AND jsonb_typeof(p_attrs) = 'object'
       AND p_attrs <> '{}'::jsonb;
$$;

ALTER TABLE work_character
    DROP CONSTRAINT IF EXISTS chk_work_character_attrs_valid;

ALTER TABLE work_character
    ADD CONSTRAINT chk_work_character_attrs_object
    CHECK (work_character_attrs_is_object(attrs));

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
            RAISE EXCEPTION 'connections 含无效角色 id %（work_id=%）', cid, NEW.work_id;
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
