# 数据库（Demo）

## 1. 安装 PostgreSQL（本机）

**macOS（Homebrew）**

```bash
brew install postgresql@15
brew services start postgresql@15
```

创建库（库名可自定，需与导入脚本一致）：

```bash
createdb quiz_verse
# 若提示无权限，可用：psql postgres -c "CREATE DATABASE quiz_verse;"
```

## 2. 建表

在仓库根目录执行：

```bash
# LoL 选手 Demo（独立表 demo_lol_player，与影视剧管线无关）
psql -d quiz_verse -f db/demo_lol_schema.sql

# 影视剧猜人物 — 通用三张表（所有作品共用）
psql -d quiz_verse -f db/work_guess_schema.sql

# 某部作品元数据（可选，首期甄嬛传）
psql -d quiz_verse -f db/seeds/zhenhuan_2011_seed.sql
```

### 2.1 影视剧通用表（不是「甄嬛传表」）

| 表 | 说明 |
| --- | --- |
| `work` | 作品注册；一行一部剧，`id` 如 `zhenhuan_2011`（**是数据主键，不是表名**） |
| `work_column` | 该作品的比对列定义（不同剧可不同列、不同枚举） |
| `work_character` | 全剧角色；`work_id` 区分归属；`attrs` 为 jsonb |

第二部剧示例：再执行一份 `db/seeds/xxx_seed.sql`（或手动 `INSERT INTO work` + `work_column`），角色仍写 `work_character`，`work_id` 为新 id 即可。

`attrs` 键须与该作品的 `work_column.attrs_path` 一致。甄嬛传字段语义见 **`docs/甄嬛传设计.md`**。

## 3. 导入 JSON

```bash
pip install -r scripts/requirements.txt
python3 scripts/import_lol_players.py
```

## 4. 校验

```bash
psql -d quiz_verse -c "SELECT COUNT(*) FROM demo_lol_player;"
psql -d quiz_verse -c "SELECT id, title_cn FROM work;"
psql -d quiz_verse -c "SELECT work_id, COUNT(*) FROM work_character GROUP BY work_id;"
```
