# Quiz Verse

猜人物题包引擎。内容以 **pack（题包）** 为单元：`pack.yaml` + `schema.yaml` + `entities.json`，服务端加载并比对，客户端只拿 brief（id / 姓名 / 别名）。

## 技术栈

| 组件 | 说明 |
|------|------|
| Java 17 | Amazon Corretto / Temurin 均可（免费 OpenJDK） |
| Spring Boot 3.3 | Web |
| Redis（可选） | 对局状态；默认内存存储，无需 Redis 即可本地跑 |
| 题包 | classpath `src/main/resources/packs/` |

## 题包

| ID | 说明 |
|----|------|
| `lol_cn_kr` | Demo：LoL 中韩选手（约 281 人） |
| `zhenhuan_2011` | 正式：甄嬛传人物 |

目录约定：

```
packs/{packId}/
  pack.yaml       # id / title / maxGuesses
  schema.yaml     # 比对列：identity | exact | set | number
  entities.json   # [{ id, name, aliases, attrs }]
```

## 本地运行

本项目需要 **JDK 17**（不要用 1.8，也不要用 26）。若本机尚未安装，可用已下载的 Corretto 17：

`/Users/captain/.jdks/amazon-corretto-17.jdk/Contents/Home`

### 命令行（推荐）

```bash
./run.sh
# 或手动：
export JAVA_HOME=$HOME/.jdks/amazon-corretto-17.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn spring-boot:run
```

### IntelliJ IDEA 绑定 JDK 17

1. **File → Project Structure → Project**
2. **SDK** → **Add SDK → JDK…**
3. 选目录：`$HOME/.jdks/amazon-corretto-17.jdk/Contents/Home`
4. 名称建议填 `corretto-17`，Language level 选 **17**
5. **Apply** 后重新打开 Maven 工具窗口，点 Reload，再 Run `QuizVerseApplication`

若弹窗只列出 1.8 / 26：点 **Add JDK**，不要选已有的 8 或 26。

- 首页：http://localhost:8098/
- 健康检查：http://localhost:8098/api/health
- LoL：http://localhost:8098/guess.html?pack=lol_cn_kr
- 甄嬛传：http://localhost:8098/guess.html?pack=zhenhuan_2011

### 对局存储

默认内存（重启丢对局）：

```bash
# application.yml 默认 quiz.match.store=memory
```

改用 Redis：

```bash
brew install redis && brew services start redis
export QUIZ_MATCH_STORE=redis
mvn spring-boot:run
```

## API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/packs` | 题包列表 |
| GET | `/api/packs/{id}/briefs` | 联想用 brief（无 attrs） |
| POST | `/api/matches` | body `{ "packId" }` 开局 |
| POST | `/api/matches/{id}/guess` | body `{ "entityId" }` 猜测 |

## 备份与旧代码

重写前完整备份：

- 本地目录：`../quiz-verse-legacy`
- Git：分支 `archive/pre-rewrite`，标签 `archive/pre-rewrite-2026-07`

产品规划仍见 `docs/`。

## 测试

```bash
mvn test
```
