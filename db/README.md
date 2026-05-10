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
psql -d quiz_verse -f db/demo_lol_schema.sql
```

## 3. 导入 JSON

```bash
pip install -r scripts/requirements.txt
# 若库名/用户不同，设置环境变量后再执行：
# export PGDATABASE=quiz_verse PGUSER=你的用户名
python3 scripts/import_lol_players.py
```

## 4. 校验

```bash
psql -d quiz_verse -c "SELECT COUNT(*) FROM demo_lol_player;"
# 期望: 281
```

下一步在 Spring Boot 中增加 `spring-boot-starter-jdbc` + `postgresql` 驱动，配置 `spring.datasource.*`，即可用 `JdbcTemplate` 或 JPA 读 `demo_lol_player` 做猜谜逻辑。
