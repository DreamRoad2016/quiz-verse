# Quiz Verse

「竞猜宇宙」——类 Wordle 的猜人物游戏后端与本地验证页。

内容以 **题包（pack）** 为单元：`pack.yaml` + `schema.yaml` + `entities.json`。服务端加载并比对，客户端只拿 brief（id / 姓名 / 别名），完整属性不下发。

## 技术栈

| 组件 | 说明 |
|------|------|
| Java 17 | Spring Boot 3.3 |
| 题包文件 | `src/main/resources/packs/` |
| 对局状态 | 默认内存；可切换 Redis |

产品规划与 ADR 见 [`docs/`](docs/)。

## 题包

| ID | 说明 |
|----|------|
| `lol_cn_kr` | Demo：LoL 中韩选手 |
| `zhenhuan_2011` | 正式：甄嬛传人物 |

```
packs/{packId}/
  pack.yaml       # 元信息
  schema.yaml     # 比对列：identity | exact | set | number
  entities.json   # [{ id, name, aliases, attrs }]
```

## 运行

需要 **JDK 17** 与 Maven。

```bash
./run.sh
# 或
mvn spring-boot:run
```

默认端口 **8098**：

- 首页：`/`
- 猜局：`/guess.html?pack={packId}`
- 健康检查：`/api/health`

对局改用 Redis：`QUIZ_MATCH_STORE=redis`（需本机 Redis 可用）。

## API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/packs` | 题包列表 |
| GET | `/api/packs/{id}/briefs` | 联想 brief（无 attrs） |
| POST | `/api/matches` | `{ "packId" }` 开局 |
| POST | `/api/matches/{id}/guess` | `{ "entityId" }` 猜测 |

## 测试

```bash
mvn test
```
