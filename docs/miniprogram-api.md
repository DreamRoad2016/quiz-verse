# 猜宇宙 · 小程序 API 与鉴权

面向微信小程序「猜宇宙」与网页试用。`aliyun` profile 默认开启鉴权。

## 鉴权流程

```text
小程序 wx.login(code)
  → POST /api/auth/wx-login { "code": "..." }
  → { "sessionToken", "expiresIn", "kind": "wx" }

网页试用
  → POST /api/auth/guest
  → { "sessionToken", "expiresIn", "kind": "guest" }

后续请求头
  Authorization: Bearer <sessionToken>
```

免鉴权：`GET /api/health`、`/api/auth/**`。

对局绑定 `ownerId`（openid 或 `guest:{ipHash}`）。他人持有 `matchId` 也无法猜/揭晓。

## 环境变量（阿里云）

| 变量 | 说明 |
| --- | --- |
| `WECHAT_APP_ID` | 小程序 AppId |
| `WECHAT_APP_SECRET` | 小程序 AppSecret（勿入库） |
| `QUIZ_SECURITY_ENABLED` | 默认 aliyun=`true`；本地默认 `false` |
| `REDIS_HOST` 等 | 会话与对局存 Redis |

启动示例：

```bash
export WECHAT_APP_ID=wx........
export WECHAT_APP_SECRET=........

nohup /usr/lib/jvm/java-17-openjdk/bin/java \
  -Xms256m -Xmx512m \
  -jar quiz-verse-0.1.0-SNAPSHOT.jar \
  --spring.profiles.active=aliyun \
  --spring.data.redis.host=127.0.0.1 \
  --spring.data.redis.port=6379 \
  > app.log 2>&1 &
```

## HTTPS 与微信合法域名

微信正式版 **不能** 使用 `http://IP:8098`。

推荐：

1. 域名解析到 ECS  
2. Nginx 监听 443，反代 `http://127.0.0.1:8098`  
3. 微信公众平台 → 开发管理 → 服务器域名 → request 合法域名填 `https://你的域名`  
4. 小程序 `miniprogram/config.js` 的 `baseUrl` 改为该 HTTPS 地址  

开发者工具可勾选「不校验合法域名」用 IP 联调。

Nginx 示例片段：

```nginx
server {
  listen 443 ssl http2;
  server_name api.example.com;
  # ssl_certificate ...;
  # ssl_certificate_key ...;

  location / {
    proxy_pass http://127.0.0.1:8098;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
  }
}
```

## 业务接口摘要

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/catalog` | **首页目录（小程序 + Web 共用）**：hot（含可配 worldIds）、hotWorlds（精选≤4）、categories（含 title/subtitle/description/ready/badge/status） |
| GET | `/api/packs` | 题包元信息列表（开局选包、调试；目录态优先用 catalog） |
| GET | `/api/packs/{packId}/briefs` | 联想/图鉴列表 |
| GET | `/api/packs/{packId}/entities/{entityId}` | 人物档案（fields + display） |
| POST | `/api/matches` | body `{ packId }` 开局 |
| POST | `/api/matches/{id}/guess` | body `{ entityId }` |
| POST | `/api/matches/{id}/give-up` | 主动揭晓 |
| GET | `/api/history/events?month=&day=&limit=` | **历史上的今天**：按月日列表（`limit` 可选）；数据来自 `content/history/days/MM-DD.json` |
| GET | `/api/history/events/{eventId}` | 历史事件详情 |

### `/api/history/events` 字段要点

- 每条：`eventId`, `year`, `month`, `day`, `title`, `description`, `tags[]`, `region`, `importance`, `eventType?`
- `tags` 允许值：`名人` / `事件` / `中国历史` / `世界历史` / `科技` / `文化` / `影视` / `体育`
- 同日排序：`importance` 降序，再 `year` 升序
- 无数据的日期返回 `[]`；未知 `eventId` 返回 404
- 网页检测页：`/vault/q/2026/history-today/hub.html`

### `/api/catalog` 字段要点

- 模板：`classpath:catalog/home.yaml`（损坏时用内置默认）
- `kind=demo` 题包不进入目录
- 宇宙有 `packId` 且服务端已加载对应官方包 → `ready=true`，`badge=可查阅`，`description` 来自 `pack.yaml`
- 否则 → `ready=false`，`badge=整理中`
- 目录未列出但已加载的官方包：按 `tags` 挂到同名分类，否则进「更多」
- **热点**：`hot.worldIds` 配置宇宙 id（如 `zhenhuan`），按序解析为 `hotWorlds`，**最多 4 个**且须已 ready；未配置则热点为空（不会自动塞满全部开放包）

限流（默认，可配 `quiz.security.*`）：guest 每 IP / 开局 / 猜测 / briefs 均有小时上限。

## 本地关闭鉴权

```bash
# 默认 application.yml 已是 QUIZ_SECURITY_ENABLED=false
./run.sh
```

## 本地开鉴权联调小程序（推荐，无需先发阿里云）

```bash
./run-mp-dev.sh
# 等同 spring.profiles.active=mpdev：security=true，match store=memory
```

小程序 `config.js` 设 `ENV = 'local'`（`http://127.0.0.1:8098`）。未配 WeChat Secret 时会走 guest。

网页可直接玩；开启鉴权后网页会通过 `/js/api.js` 自动 guest 登录。
