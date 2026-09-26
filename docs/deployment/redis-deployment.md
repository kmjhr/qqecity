# Redis 部署文档

> 本文档覆盖 Redis 7.x 的安装、配置、远程访问与常见问题，支持 Windows / Linux 双平台。
> Redis 在项目中用于 **JWT Token 黑名单** 与缓存，**推荐安装**。未安装时后端自动降级（不影响登录，仅登出后 Token 不会立即失效）。

---

## 目录

- [一、Windows 部署](#一windows-部署)
- [二、Linux 部署](#二linux-部署)
- [三、开启远程访问](#三开启远程访问)
- [四、项目配置示例](#四项目配置示例)
- [五、验证脚本](#五验证脚本)
- [六、常见问题排查](#六常见问题排查)

---

## 一、Windows 部署

### 安装步骤

1. 下载 Redis for Windows（推荐 7.x）：<https://github.com/tporadowski/redis/releases>
2. 解压到 `D:\redis\`
3. 编辑 `redis.windows.conf`（开发环境默认即可，无需密码）：

```conf
# 端口
port 6379
# 绑定地址（本地开发默认 127.0.0.1）
bind 127.0.0.1
# 持久化（AOF）
appendonly yes
# 密码（生产环境必须设置；本地开发可留空）
# requirepass yourpassword
```

4. 启动 Redis：

```cmd
# 前台启动（调试用，关掉窗口就停）
cd /d D:\redis
redis-server.exe redis.windows.conf

# 或注册为 Windows 服务（推荐，后台运行）
redis-server.exe --service-install redis.windows.conf --service-name redis
net start redis
```

5. 验证：

```cmd
redis-cli ping
# 返回 PONG 即正常
```

---

## 二、Linux 部署

### CentOS 7 / RockyLinux / AlmaLinux

```bash
# EPEL 源安装
yum install -y epel-release
yum install -y redis

# 启动并设置开机自启
systemctl start redis
systemctl enable redis

# 验证
redis-cli ping
```

### Ubuntu 20.04 / 22.04

```bash
sudo apt update
sudo apt install -y redis-server

# 启动并设置开机自启
sudo systemctl start redis-server
sudo systemctl enable redis-server

# 验证
redis-cli ping
```

### 源码编译安装（最新版）

如果包管理器版本较旧，可源码编译安装最新稳定版：

```bash
wget https://download.redis.io/redis-stable.tar.gz
tar xzf redis-stable.tar.gz
cd redis-stable
make
make install

# 配置文件
mkdir -p /etc/redis
cp redis.conf /etc/redis/redis.conf

# systemd 服务（可选）
# 参考：https://redis.io/docs/getting-started/installation/install-redis-on-linux/
```

---

## 三、开启远程访问

> ⚠️ **安全警告**：Redis 默认无密码且只绑定 127.0.0.1。开启远程访问**必须设置密码**，否则极易被入侵。
> 以下配置仅适用于内网开发测试环境，生产环境请严格加固。

### 第 1 步：设置密码

编辑 Redis 配置文件（Windows: `redis.windows.conf`，Linux: `/etc/redis.conf` 或 `/etc/redis/redis.conf`）：

```conf
# 取消注释并设置密码
requirepass your_redis_password
```

### 第 2 步：允许远程连接

```conf
# 注释掉 bind 127.0.0.1，或改为 0.0.0.0
bind 0.0.0.0

# 生产环境建议保留 protected-mode yes，配合密码使用
# 内网开发且设了密码的话，protected-mode 不影响
protected-mode yes
```

重启 Redis 服务：
- Windows：`net stop redis && net start redis`
- Linux：`systemctl restart redis`

### 第 3 步：防火墙放行 6379 端口

**CentOS：**
```bash
firewall-cmd --zone=public --add-port=6379/tcp --permanent
firewall-cmd --reload
```

**Ubuntu：**
```bash
sudo ufw allow 6379/tcp
sudo ufw reload
```

### 生产环境加固清单

- [ ] **设置强密码**（`requirepass`，16 位以上随机字符串）
- [ ] **绑定内网 IP**（不要 `bind 0.0.0.0`，绑定具体内网地址）
- [ ] **重命名危险命令**（`FLUSHDB`、`FLUSHALL`、`CONFIG`、`KEYS` 等）
- [ ] **限制最大连接数**（`maxclients`）
- [ ] **开启 AOF 持久化**（`appendonly yes`）
- [ ] **防火墙 IP 白名单**（只允许应用服务器 IP 访问 6379）

---

## 四、项目配置示例

### Spring Boot application.yml

本地开发 Redis 默认**无密码**，保持以下配置即可：

```yaml
spring:
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password: ${SPRING_DATA_REDIS_PASSWORD:}   # 空表示无密码
      timeout: 3000ms
```

如果设置了密码，通过环境变量注入或直接修改：

```yaml
spring:
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password: "你的Redis密码"
```

> Redis 不可用时，后端会自动降级并打印警告日志，不影响登录和核心业务功能。

### Docker Compose 方式

项目根目录 `docker-compose.yml` 已配置 Redis 服务，一键启动：

```bash
docker compose up -d redis
```

- 端口：`6379`（映射到宿主机）
- 密码：无（开发环境默认）
- 持久化：AOF 已开启

---

## 五、验证脚本

项目提供 Redis 验证脚本，一键检查连接、读写、持久化：

**Windows：**
```cmd
cd scripts\windows
03-verify-redis.bat
```

**Linux：**
```bash
cd scripts/linux
./03-verify-redis.sh
```

验证项：连接测试、PING、SET/GET 读写、info server 版本查询、持久化配置检查。

---

## 六、常见问题排查

| 报错信息 | 原因 | 解决 |
|----------|------|------|
| `Unable to connect to Redis` / 连接超时 | Redis 未启动 / 端口不对 / bind 限制 | 检查服务状态、端口 6379、bind 地址 |
| `NOAUTH Authentication required` | Redis 设了密码但配置没填 | 在 application.yml 的 `spring.data.redis.password` 填入密码 |
| `DENIED Redis is running in protected mode` | 远程访问 + 无密码 + 保护模式 | 设置密码（推荐），或内网测试时设 `protected-mode no` |
| `WRONGPASS invalid username-password pair` | 密码错误 | 检查 Redis 密码与配置是否一致 |
| 后端启动报"Redis 连接失败"警告 | Redis 未启动或配置不对 | 启动 Redis 或检查配置；不影响核心功能使用 |
| 内存不足 / OOM | 数据量超过 maxmemory | 配置 `maxmemory` 和 `maxmemory-policy`（如 allkeys-lru） |
| `MISCONF Redis is configured to save RDB snapshots` | 持久化失败（磁盘满/权限问题） | 检查磁盘空间、数据目录权限，或临时 `config set stop-writes-on-bgsave-error no` |
| Redis 经常被入侵挖矿 | 公网暴露且无密码 / 弱密码 | 立即设置强密码、绑定内网 IP、防火墙限制来源 IP |

> 更多问题见 [05-troubleshooting.md](./05-troubleshooting.md)。
