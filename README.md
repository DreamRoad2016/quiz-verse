# Quiz Verse

「猜人物」类玩法后端（产品规划与数据模型见 `person_memory/guess_all/`）。当前仓库仍保留 **10×10 方格猜飞机** 的完整 Demo（**Spring Boot + Redis**）；**PostgreSQL** 侧已提供 **LoL 选手 Demo 表 + 281 条导入数据**，装好库并执行脚本即可；应用内 JDBC 读写下一步再接。

## 技术栈

| 组件 | 说明 |
|------|------|
| Java 8 | 与 `pom.xml` 一致，后续可升级 |
| Spring Boot 2.6.8 | Web + Redis |
| Redis | 对局 / 会话（猜飞机逻辑依赖） |
| PostgreSQL | 已接 JDBC（captain 读 `demo_lol_player`） |

## 目录结构

```
quiz-verse/
├── pom.xml
├── Dockerfile.dev
├── README.md
├── db/                     # PostgreSQL 建表脚本（Demo 选手表）
├── scripts/
│   ├── data/
│   │   └── lol-players-cn-kr.json   # 抓包/下载的 CN_KR 题库（281 条）
│   ├── import_lol_players.py        # 导入 PG
│   └── requirements.txt
└── src/main/
    ├── java/net/qihoo/guessthepattern/
    │   ├── GuessThePatternApplication.java   # 启动类（包名历史遗留）
    │   ├── config/                           # Web、CORS、Knife4j、Tomcat（可选双端口）
    │   ├── web/                              # GameController、UserController
    │   ├── service/ model/ dto/ …
    └── resources/
        ├── application.yml                   # 默认本机 Redis + 端口 8098
        └── static/
            ├── index.html                   # 竞猜宇宙首页
            ├── guess-lol.html               # 英雄联盟选手猜谜页
            └── plane.html                   # 小游戏 · 猜飞机（入口在首页页脚）
```

**接口文档**：启动后打开 **http://localhost:8098/doc.html**

**猜选手单人 Demo 页面**：**http://localhost:8098/guess-lol.html**  
接口：`GET /api/lol-guess/start`、`GET /api/lol-guess/guess?matchId=&playerId=`（`playerId` 为题库 UUID）。

## 本地运行

### 1. Redis

```bash
# macOS
brew install redis
brew services start redis

# 或 Docker
docker run -d --name quiz-verse-redis -p 6379:6379 redis:7-alpine
```

### 2. 启动

```bash
mvn spring-boot:run
# 若已修复 Maven Wrapper：./mvnw spring-boot:run
```

浏览器：**http://localhost:8098/**（竞猜宇宙首页）  
猜选手：**http://localhost:8098/guess-lol.html**  
猜飞机（角落入口）：**http://localhost:8098/plane.html**  
健康检查：`GET http://localhost:8098/game/hello`

### 3. 自定义 Redis（可选）

```bash
export REDIS_HOST=127.0.0.1
export REDIS_PORT=6379
export REDIS_PASSWORD=
export REDIS_DATABASE=0
./mvnw spring-boot:run
```

## 与旧 Demo 的差异

- 已去掉原 `application.yml` 中的**内网 Redis 地址与密码**。
- 默认 **单端口 8098**；需要双端口时在 `application.yml` 增加 `server.http-port`。
- Maven `artifactId` 为 **quiz-verse**，打包：`target/quiz-verse-0.0.1-SNAPSHOT.jar`。

## PostgreSQL：Demo 选手数据（猜人物流程验证）

已抓取 **guessassin.xyz** 公开接口 `GET /api/players/CN_KR` 的完整 JSON（**281** 条中韩赛区选手），用于在本地走通「**库里有题 → 后端读库 → 比对逻辑**」，与最终「历史人物 / 明星」题库无关，仅作技术占位。

| 步骤 | 说明 |
|------|------|
| 数据文件 | `scripts/data/lol-players-cn-kr.json` |
| 建表 | `db/demo_lol_schema.sql` → 表 `demo_lol_player` |
| 导入 | `python3 scripts/import_lol_players.py`（依赖见 `scripts/requirements.txt`） |
| 详细命令 | 见 **`db/README.md`** |

**合规**：数据仅供本机开发自测，勿对外分发或商用。

### 接 Spring Boot（下一步）

1. `pom.xml` 增加 `spring-boot-starter-jdbc` + `org.postgresql:postgresql`。  
2. `application.yml` 增加 `spring.datasource.url/username/password`。  
3. 写 `JdbcTemplate` 或 Repository 读 `demo_lol_player`，实现「开一局 / 猜一次 / 返回绿黄灰」API。
