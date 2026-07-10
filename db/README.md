# 数据库（Demo）

## 影视剧猜人物

```bash
psql -d quiz_verse -f db/work_guess_schema.sql
psql -d quiz_verse -f db/seeds/zhenhuan_2011_seed.sql
```

旧库：`psql -d quiz_verse -f db/migrations/003_attrs_config_v3.sql`

| 表 | 说明 |
| --- | --- |
| `work` | 作品；`config_dir` 指向 `config/works/{id}` |
| `work_character` | `display_name`、`call_names`（称呼）、`attrs` jsonb |
| `work_column` | 比对列规则 |

**字段选项（性别/位分/剧情等）**：编辑 [`config/works/zhenhuan_2011/`](../config/works/zhenhuan_2011/) 下 json，见 [`config/README.md`](../config/README.md)。

录入步骤：[`docs/甄嬛传设计.md`](../docs/甄嬛传设计.md) §5。

## LoL Demo

```bash
psql -d quiz_verse -f db/demo_lol_schema.sql
python3 scripts/import_lol_players.py
```
