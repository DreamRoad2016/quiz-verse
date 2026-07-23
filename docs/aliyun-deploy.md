# 阿里云部署与运维手册（quiz-verse）

面向当前生产机实际环境整理，避免以后忘记操作顺序。

## 环境概况

| 项 | 值 |
|---|---|
| 服务器目录 | `/home/admin/app` |
| 系统 | Alibaba Cloud Linux 3 |
| JDK（本服务） | `/usr/lib/jvm/java-17-openjdk/bin/java`（**必须 17**） |
| 系统默认 `java` | 仍为 8，给其它旧服务用，**不要**用它启本服务 |
| Redis | 本机 `127.0.0.1:6379`（已安装，`redis-cli ping` → `PONG`） |
| 应用端口 | `8098` |
| Spring Profile | `aliyun`（对局走 Redis） |
| 当前 jar | `quiz-verse-0.1.0-SNAPSHOT.jar` |

访问：

- 首页：`http://<服务器公网IP>:8098/`
- 猜题：`http://<服务器公网IP>:8098/guess.html?pack=zhenhuan_2011`
- 健康检查：`http://127.0.0.1:8098/api/health`

---

## 日常发布（云效出包后）

### 1. 拿到包并解压到工作目录

云效产物一般是 `package.tgz`，上传/拉取到服务器后：

```bash
cd /home/admin/app
# 如有旧 package.tgz，可先备份
# mv package.tgz package.tgz.bak.$(date +%Y%m%d%H%M)

# 放入新的 package.tgz 后解压（会覆盖同目录下 jar 等文件，注意确认）
tar -zxvf package.tgz
ls -l quiz-verse-*.jar
```

确认新 jar 存在，例如：

```text
quiz-verse-0.1.0-SNAPSHOT.jar
```

### 2. 停旧进程

```bash
ps -ef | grep quiz-verse | grep -v grep
```

记下 PID，例如 `1370639`，然后：

```bash
kill <PID>
sleep 2
ps -ef | grep quiz-verse | grep -v grep
```

若还在：

```bash
kill -9 <PID>
```

也可一键停（慎用，确认只有本服务一个 java 进程时）：

```bash
pkill -f 'quiz-verse-.*-SNAPSHOT.jar' || true
```

### 3. 启动新版本

在 `/home/admin/app` 下执行：

```bash
cd /home/admin/app

nohup /usr/lib/jvm/java-17-openjdk/bin/java \
  -Xms256m -Xmx512m \
  -jar quiz-verse-0.1.0-SNAPSHOT.jar \
  --spring.profiles.active=aliyun \
  --spring.data.redis.host=127.0.0.1 \
  --spring.data.redis.port=6379 \
  > app.log 2>&1 &
```

若 Redis 设置了密码，追加：

```bash
  --spring.data.redis.password=你的密码 \
```

（写在 `-jar` 参数同一段 `nohup ... &` 里。）

jar 文件名若随版本变化，把上面的 `quiz-verse-0.1.0-SNAPSHOT.jar` 改成实际文件名。

### 4. 验收

```bash
ps -ef | grep quiz-verse | grep -v grep
# 应看到 java-17-openjdk ... quiz-verse-...jar ... aliyun

tail -n 80 app.log
# 应有 Tomcat started on port 8098，且无 Redis 连接错误

curl -s http://127.0.0.1:8098/api/health
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8098/
# 期望 200
```

浏览器打开首页与猜题页，开一局确认能猜、能结束。

---

## 常用运维命令

```bash
# 看进程
ps -ef | grep quiz-verse | grep -v grep

# 看日志
cd /home/admin/app
tail -f app.log

# Redis
redis-cli ping

# JDK 17 是否还在
/usr/lib/jvm/java-17-openjdk/bin/java -version
```

---

## 回滚（出问题切回旧包）

目录里若保留了备份，例如 `quiz-verse-0.0.1-SNAPSHOTbak.jar`：

1. 按上面步骤停当前进程  
2. 用备份 jar 启动（**注意**：很老的包可能不是 0.1.0 / 未必支持 `aliyun` profile，按当时能跑的命令回退）  
3. 新版本正常后，再决定是否删备份  

更稳妥：每次发版前：

```bash
cd /home/admin/app
cp -a quiz-verse-0.1.0-SNAPSHOT.jar "quiz-verse-0.1.0-SNAPSHOT.bak.$(date +%Y%m%d%H%M).jar"
```

---

## 云效构建注意

- 构建机使用 **JDK 17** + Maven  
- 构建命令示例：`mvn -B -DskipTests clean package`  
- 产物 jar：`target/quiz-verse-0.1.0-SNAPSHOT.jar`（再打进 `package.tgz` 发到 `/home/admin/app`）  
- 仓库：`main` 分支；发版前确认已 push  

---

## 排障速查

| 现象 | 处理 |
|---|---|
| `UnsupportedClassVersionError` / class file 61 | 没用上 JDK 17，检查启动命令是否为 `/usr/lib/jvm/java-17-openjdk/bin/java` |
| 启动报 Redis / Connection refused | `redis-cli ping`；检查 host/port/password |
| 端口占用 | `ss -lntp \| grep 8098`，停掉旧进程再建 |
| 页面能开但对局异常 | 确认带了 `--spring.profiles.active=aliyun` |
| 外网打不开 | 安全组放行 **8098**，或查 Nginx 反代是否指向 `127.0.0.1:8098` |

---

## 一次完整发布速查（复制用）

```bash
cd /home/admin/app
tar -zxvf package.tgz
pkill -f 'quiz-verse-.*-SNAPSHOT.jar' || true
sleep 2

nohup /usr/lib/jvm/java-17-openjdk/bin/java \
  -Xms256m -Xmx512m \
  -jar quiz-verse-0.1.0-SNAPSHOT.jar \
  --spring.profiles.active=aliyun \
  --spring.data.redis.host=127.0.0.1 \
  --spring.data.redis.port=6379 \
  > app.log 2>&1 &

sleep 3
ps -ef | grep quiz-verse | grep -v grep
tail -n 40 app.log
curl -s http://127.0.0.1:8098/api/health
```

文档路径（本仓库）：`docs/aliyun-deploy.md`。
