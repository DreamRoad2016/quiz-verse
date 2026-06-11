-- 《甄嬛传》作品元数据（work + work_column），非建表脚本
-- 设计：docs/甄嬛传设计.md
-- 前置：psql -d quiz_verse -f db/work_guess_schema.sql
-- 使用：psql -d quiz_verse -f db/seeds/zhenhuan_2011_seed.sql

BEGIN;

INSERT INTO work (id, title_cn, category, pool_type, schema_version)
VALUES ('zhenhuan_2011', '甄嬛传', 'drama', 'single_work', 1)
ON CONFLICT (id) DO UPDATE SET
    title_cn = EXCLUDED.title_cn,
    updated_at = now();

INSERT INTO work_column (
    work_id, column_key, label_cn, value_type, compare_rule, attrs_path,
    in_guess_table, sort_order, enum_values, description
) VALUES
    ('zhenhuan_2011', 'gender', '性别', 'enum', 'exact', 'gender',
     TRUE, 10, '["男","女"]'::jsonb, '相同=绿，不同=灰'),
    ('zhenhuan_2011', 'role_type', '身份类别', 'enum', 'exact', 'role_type',
     TRUE, 20, '["后宫嫔妃","皇族","宫女","太监","太医","侍卫","前朝官员","嬷嬷"]'::jsonb,
     '相同=绿，不同=灰'),
    ('zhenhuan_2011', 'titles', '位分/身份', 'text_array', 'set_overlap', 'titles',
     TRUE, 30, NULL, '完全一致=绿，有交集=黄，无交集=灰'),
    ('zhenhuan_2011', 'residences', '住所', 'text_array', 'set_overlap', 'residences',
     TRUE, 40, NULL, '完全一致=绿，有交集=黄，无交集=灰'),
    ('zhenhuan_2011', 'connections', '关系网', 'bigint_array', 'set_overlap', 'connections',
     TRUE, 50, NULL, '存 work_character.id；完全一致=绿，有交集=黄，无交集=灰')
ON CONFLICT (work_id, column_key) DO UPDATE SET
    label_cn = EXCLUDED.label_cn,
    value_type = EXCLUDED.value_type,
    compare_rule = EXCLUDED.compare_rule,
    attrs_path = EXCLUDED.attrs_path,
    in_guess_table = EXCLUDED.in_guess_table,
    sort_order = EXCLUDED.sort_order,
    enum_values = EXCLUDED.enum_values,
    description = EXCLUDED.description;

DELETE FROM work_column
WHERE work_id = 'zhenhuan_2011' AND column_key IN ('age_group', 'is_royal');

COMMIT;

-- 角色数据写入 work_character（work_id = zhenhuan_2011），示例：
-- INSERT INTO work_character (work_id, display_name, aliases, attrs) VALUES (
--   'zhenhuan_2011', '甄嬛', ARRAY['莞常在','熹贵妃'],
--   '{"gender":"女","role_type":"后宫嫔妃","titles":["常在","贵妃"],"residences":["碎玉轩"],"connections":[]}'::jsonb
-- );
