-- 甄嬛传：注册作品 + 比对列（选项内容见 config/works/zhenhuan_2011/）
-- 前置：psql -d quiz_verse -f db/work_guess_schema.sql

BEGIN;

INSERT INTO work (id, title_cn, category, pool_type, schema_version, config_dir)
VALUES (
    'zhenhuan_2011', '甄嬛传', 'drama', 'single_work', 3,
    'config/works/zhenhuan_2011'
)
ON CONFLICT (id) DO UPDATE SET
    title_cn = EXCLUDED.title_cn,
    schema_version = EXCLUDED.schema_version,
    config_dir = EXCLUDED.config_dir,
    updated_at = now();

INSERT INTO work_column (
    work_id, column_key, label_cn, value_type, compare_rule, attrs_path,
    in_guess_table, sort_order, description
) VALUES
    ('zhenhuan_2011', 'gender', '性别', 'enum', 'exact', 'gender',
     TRUE, 10, '单选；选项 enums/gender.json'),
    ('zhenhuan_2011', 'role_type', '身份类别', 'enum', 'exact', 'role_type',
     TRUE, 20, '单选；选项 enums/role_type.json'),
    ('zhenhuan_2011', 'titles', '位分', 'text_array', 'set_overlap', 'titles',
     TRUE, 30, '多选；选项 enums/titles.json；不是称呼'),
    ('zhenhuan_2011', 'residences', '住所', 'text_array', 'set_overlap', 'residences',
     TRUE, 40, '多选；选项 enums/residences.json'),
    ('zhenhuan_2011', 'major_plots', '重大剧情', 'text_array', 'set_overlap', 'major_plots',
     TRUE, 50, '多选；存 plot key；选项 enums/major_plots.json'),
    ('zhenhuan_2011', 'connections', '关系网', 'bigint_array', 'set_overlap', 'connections',
     TRUE, 60, '多选其他角色 id')
ON CONFLICT (work_id, column_key) DO UPDATE SET
    label_cn = EXCLUDED.label_cn,
    value_type = EXCLUDED.value_type,
    compare_rule = EXCLUDED.compare_rule,
    attrs_path = EXCLUDED.attrs_path,
    in_guess_table = EXCLUDED.in_guess_table,
    sort_order = EXCLUDED.sort_order,
    description = EXCLUDED.description;

DELETE FROM work_column
WHERE work_id = 'zhenhuan_2011'
  AND column_key IN ('age_group', 'is_royal');

COMMIT;

-- 示例（称呼 vs 位分 分开）：
-- INSERT INTO work_character (work_id, display_name, call_names, attrs, status, is_active) VALUES (
--   'zhenhuan_2011', '甄嬛',
--   ARRAY['嬛嬛','熹贵妃','莞莞'],
--   '{
--     "gender": "女",
--     "role_type": "后宫嫔妃",
--     "titles": ["常在","贵人","嫔","妃","贵妃"],
--     "residences": ["碎玉轩","永寿宫"],
--     "major_plots": ["jinghong_wu","dixue_qinzi"],
--     "connections": [2, 3]
--   }'::jsonb,
--   'ready', true
-- );
