-- 升级到 schema v3：称呼列、剧情回 attrs、去掉剧情关联表
-- 若全新安装只需 work_guess_schema.sql，可跳过
-- psql -d quiz_verse -f db/migrations/003_attrs_config_v3.sql

BEGIN;

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

ALTER TABLE work ADD COLUMN IF NOT EXISTS config_dir VARCHAR(256);

ALTER TABLE work_column DROP COLUMN IF EXISTS data_source;
ALTER TABLE work_column DROP COLUMN IF EXISTS enum_values;

DROP VIEW IF EXISTS v_work_character_plot_ids;
DROP FUNCTION IF EXISTS work_plot_set_characters(VARCHAR, BIGINT, BIGINT[]);
DROP TABLE IF EXISTS work_character_plot CASCADE;
DROP TABLE IF EXISTS work_plot CASCADE;

UPDATE work SET config_dir = 'config/works/' || id WHERE config_dir IS NULL;

COMMIT;
