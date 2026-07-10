-- 将影视剧表所有者改为 captain（与 demo_lol_player、application.yml 默认用户一致）
-- 须由当前表拥有者或超级用户执行，例如：
--   psql -d quiz_verse -U zhouqingji -f db/fix_work_tables_owner_captain.sql

ALTER TABLE IF EXISTS work OWNER TO captain;
ALTER TABLE IF EXISTS work_column OWNER TO captain;
ALTER TABLE IF EXISTS work_character OWNER TO captain;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_class WHERE relname = 'work_character_id_seq') THEN
        ALTER SEQUENCE work_character_id_seq OWNER TO captain;
    END IF;
END $$;

GRANT ALL PRIVILEGES ON TABLE work TO captain;
GRANT ALL PRIVILEGES ON TABLE work_column TO captain;
GRANT ALL PRIVILEGES ON TABLE work_character TO captain;

GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO captain;
