# Quiz Verse

「猜人物」类玩法后端（产品规划与数据模型见 `person_memory/guess_all/`）。当前仓库仍保留 **10×10 方格猜飞机** 的完整 Demo，用于验证 **Spring Boot + Redis**；后续接入 **PostgreSQL** 与新 API 时，再改依赖与 `application.yml` 即可。

## 技术栈

| 组件 | 说明 |
|------|------|
| Java 8 | 与 `pom.xml` 一致，后续可升级 |
| Spring Boot 2.6.8 | Web + Redis |
| Redis | 对局 / 会话（猜飞机逻辑依赖） |
| PostgreSQL | **未接入**，本地装好后再加 JDBC 配置 |

## 目录结构

```
quiz-verse/
├── pom.xml
├── Dockerfile.dev
├── README.md
└── src/main/
    ├── java/net/qihoo/guessthepattern/
    │   ├── GuessThePatternApplication.java   # 启动类（包名历史遗留）
    │   ├── config/                           # Web、CORS、Knife4j、Tomcat（可选双端口）
    │   ├── web/                              # GameController、UserController
    │   ├── service/ model/ dto/ …
    └── resources/
        ├── application.yml                   # 默认本机 Redis + 端口 8098
        └── static/index.html                 # 猜飞机前端（后续可换）
```

**接口文档**：启动后打开 **http://localhost:8098/doc.html**

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
./mvnw spring-boot:run
```

浏览器：**http://localhost:8098/**  
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

## PostgreSQL（后续步骤）

1. 安装 PG 15+，建库与用户。  
2. `pom.xml` 增加 `spring-boot-starter-jdbc` + `postgresql` 驱动。  
3. `application.yml` 增加 `spring.datasource.*`。  
4. 与猜飞机逻辑可并行开发，互不影响。
