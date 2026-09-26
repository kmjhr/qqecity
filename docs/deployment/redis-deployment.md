# 青启e城 · Redis 部署文档

> 适用场景：在新机器上从零部署青启e城项目 Redis 缓存服务，支持 Windows / Linux 双平台。
> 默认无密码（内网测试用），生产环境请务必设置强密码并限制访问。

---

## ⚠️ 安全声明

> **Redis 默认无密码且允许远程访问，仅适用于内网测试环境，生产环境绝对不安全！**
>
> - 生产环境必须设置强密码（16 位以上随机字符串）
> - 生产环境必须绑定 `127.0.0.1` 或内网 IP，禁止 `0.0.0.0`
> - 生产环境建议配合防火墙只放行指定 IP 段
> - 生产环境建议禁用 `CONFIG` 等危险命令（rename-command）
> - 生产环境建议开启 RDB + AOF 双持久化

---

## 目录

- [一、Redis 在项目中的作用](#一redis-在项目中的作用)
- [二、Windows 部署流程](#二windows-部署流程)
- [三、Linux 部署流程](#三linux-部署流程)
- [四、开启远程访问（内网测试用）](#四开启远程访问内网测试用)
- [五、项目配置文件示例](#五项目配置文件示例)
- [六、常见报错排查](#六常见报错排查)

---

## 一、Redis 在项目中的作用

青启e城项目中 Redis 的用途（预留，MVP 阶段可选择性启用）：

| 用途 | 对应模块 | 说明 |
|------|----------|------|
| Token 黑名单 | 用户模块 | JWT 登出时将 token 加入黑名单 |
| 验证码缓存 | 用户模块 | 短信/图形验证码存储与过期 |
| 接口限流 | 公共模块 | 防刷限流（按用户/IP 维度） |
| 热点数据缓存 | 业务模块 | 反诈内容、商户列表等读多写少数据 |
| 会话状态 | 消息模块 | 未读消息计数缓存 |
| 分布式锁 | 全局 | 防止重复提交、并发控制 |

> MVP 阶段 Redis 为可选依赖，不启用也能跑通核心业务流程。

---

## 二、Windows 部署流程

### 2.1 下载 Redis

Redis 官方不支持 Windows，推荐使用 **Microsoft Archive 版本** 或 **Memurai**（Windows 原生兼容版）。

**方式一：Redis for Windows（推荐，免费）**

- 下载地址：<https://github.com/tporadowski/redis/releases>
- 选择最新版的 `.msi` 安装包或 `.zip` 压缩包
- 版本建议：Redis 5.0.x（Windows 版本停更在 5.x，但完全够用）

**方式二：Memurai（官方推荐的 Windows 替代品）**

- 下载地址：<https://www.memurai.com/get-memurai
- 开发者版免费，兼容 Redis 协议

### 2.2 方式 A：ZIP 免安装版

**Step 1：解压**

```
解压到 D:\Redis-x64-5.0.x\
```

**Step 2：修改配置文件**

编辑 `redis.windows.conf`，找到并修改以下配置：

```conf
# 绑定地址（默认 127.0.0.1，远程访问需改成 0.0.0.0）
bind 127.0.0.1

# 端口（默认 6379）
port 6379

# 设置密码（内网测试可注释掉，生产环境必须设置）
# requirepass 123456

# 数据库数量（默认16个，用第0个就行）
databases 16

# RDB 持久化（开启快照保存）
save 900 1
save 300 10
save 60 10000

# 持久化文件名
dbfilename dump.rdb

# 数据目录
dir ./

# 日志文件
logfile "redis_log.txt"

# 最大内存（建议设为物理内存的 1/4 ~ 1/2）
# maxmemory 256mb

# 内存淘汰策略（超过 maxmemory 后怎么处理）
# maxmemory-policy allkeys-lru
```

**Step 3：启动 Redis**

```cmd
cd D:\Redis-x64-5.0.x
redis-server.exe redis.windows.conf
```

看到以下画面即启动成功：

```
                _._
           _.-``__ ''-._
      _.-``    `.  `_.  ''-._           Redis 5.0.x
  .-`` .-```.  ```\/    _.,_ ''-._
 (    '      ,       .-`  | `,    )     Running in standalone mode
 |`-._`-...-` __...-.``-._|'` _.-'|     Port: 6379
 |    `-._   `._    /     _.-'    |     PID: xxxx
  `-._    `-._  `-./  _.-'    _.-'
 |`-._`-._    `-.__.-'    _.-'_.-'|
 |    `-._`-._        _.-'_.-'    |
  `-._    `-._`-.__.-'_.-'    _.-'
 |`-._`-._    `-.__.-'    _.-'_.-'|
 |    `-._`-._        _.-'_.-'    |
  `-._    `-._`-.__.-'_.-'    _.-'
      `-._    `-.__.-'    _.-'
          `-._        _.-'
              `-.__.-'
```

**Step 4（可选）：注册为 Windows 服务**

让 Redis 开机自启：

```cmd
# 注册服务
redis-server --service-install redis.windows.conf --service-name Redis

# 启动服务
redis-server --service-start --service-name Redis

# 停止服务
redis-server --service-stop --service-name Redis

# 卸载服务
redis-server --service-uninstall --service-name Redis
```

### 2.3 方式 B：MSI 安装版

1. 双击 `.msi` 安装包
2. 勾选 "Add Redis installation folder to the PATH"（添加到环境变量）
3. 设置端口（默认 6379）
4. 设置最大内存（可跳过，后续改配置文件）
5. 完成安装，服务会自动启动

### 2.4 验证安装

新开一个 cmd 窗口：

```cmd
redis-cli -h 127.0.0.1 -p 6379
```

进入交互模式后：

```
127.0.0.1:6379> ping
PONG
127.0.0.1:6379> set test hello
OK
127.0.0.1:6379> get test
"hello"
127.0.0.1:6379> exit
```

能返回 `PONG` 即安装成功。

---

## 三、Linux 部署流程

以下以 **CentOS 7/8** 和 **Ubuntu 20.04/22.04** 为例。

### 3.1 CentOS 7 / RockyLinux / AlmaLinux

**方式一：yum 安装（简单）**

```bash
# 安装 EPEL 源（Redis 在 EPEL 里）
yum install -y epel-release

# 安装 Redis
yum install -y redis

# 启动并设置开机自启
systemctl start redis
systemctl enable redis
systemctl status redis
```

**方式二：源码编译（推荐，版本新）**

```bash
# 安装编译依赖
yum install -y gcc make wget

# 下载源码（以 7.2.x 为例，去官网找最新稳定版）
cd /usr/local/src
wget https://download.redis.io/releases/redis-7.2.5.tar.gz

# 解压编译
tar -zxvf redis-7.2.5.tar.gz
cd redis-7.2.5
make
make install PREFIX=/usr/local/redis

# 复制配置文件
mkdir -p /usr/local/redis/conf
cp redis.conf /usr/local/redis/conf/

# 创建数据目录
mkdir -p /data/redis
```

配置 systemd 服务：

```bash
cat > /etc/systemd/system/redis.service << 'EOF'
[Unit]
Description=Redis Server
After=network.target

[Service]
Type=forking
ExecStart=/usr/local/redis/bin/redis-server /usr/local/redis/conf/redis.conf
ExecStop=/usr/local/redis/bin/redis-cli shutdown
Restart=always
RestartSec=5
User=root
Group=root

[Install]
WantedBy=multi-user.target
EOF
```

修改配置：

```bash
vi /usr/local/redis/conf/redis.conf
```

修改关键项：

```conf
bind 127.0.0.1
port 6379
daemonize yes
pidfile /var/run/redis_6379.pid
dir /data/redis
logfile /data/redis/redis.log
# requirepass yourpassword
```

启动服务：

```bash
systemctl daemon-reload
systemctl start redis
systemctl enable redis
systemctl status redis
```

### 3.2 Ubuntu 20.04 / 22.04

**方式一：apt 安装（简单）**

```bash
sudo apt update
sudo apt install -y redis-server

# 启动服务（一般安装后自动启动）
sudo systemctl start redis-server
sudo systemctl enable redis-server
sudo systemctl status redis-server
```

**方式二：源码编译（同 CentOS）**

```bash
# 安装编译依赖
sudo apt install -y build-essential wget

# 下载编译
cd /usr/local/src
sudo wget https://download.redis.io/releases/redis-7.2.5.tar.gz
sudo tar -zxvf redis-7.2.5.tar.gz
cd redis-7.2.5
sudo make
sudo make install PREFIX=/usr/local/redis
```

后续配置和 systemd 服务与 CentOS 相同。

### 3.3 Linux 验证安装

```bash
redis-cli ping
# 返回 PONG 即为成功
```

或：

```bash
redis-cli
127.0.0.1:6379> ping
PONG
127.0.0.1:6379> info server
# 查看 Redis 版本等信息
```

---

## 四、开启远程访问（内网测试用）

> ⚠️ **重要安全提示**
> - 以下操作**仅适用于内网测试/开发环境**
> - 生产环境严禁开放 Redis 到公网，必须绑定内网 IP 并设强密码
> - 生产环境建议配合防火墙只放行指定 IP 段
> - 生产环境建议重命名危险命令（如 `CONFIG`、`FLUSHALL`、`KEYS`）

### 4.1 修改绑定地址

**Windows：**

编辑 `redis.windows.conf`：

```conf
# 注释掉 bind 127.0.0.1，或改为 0.0.0.0
# bind 127.0.0.1
bind 0.0.0.0
```

**Linux：**

```bash
vi /etc/redis.conf      # CentOS yum 安装
# 或
vi /usr/local/redis/conf/redis.conf   # 源码编译
```

修改：

```conf
# 允许所有 IP 访问（内网测试用）
bind 0.0.0.0
# 关闭保护模式（无密码远程访问需要关闭，仅内网测试用）
protected-mode no
```

重启 Redis 服务：

```bash
# CentOS
systemctl restart redis

# Ubuntu
systemctl restart redis-server
```

### 4.2 设置密码

内网测试也建议设置密码：

```conf
requirepass 123456
```

> 生产环境密码建议 16 位以上随机字符串。

### 4.3 防火墙放行 6379 端口

**Windows 防火墙：**

```powershell
# 以管理员身份运行 PowerShell
New-NetFirewallRule -DisplayName "Redis 6379" -Direction Inbound -Protocol TCP -LocalPort 6379 -Action Allow
```

**CentOS（firewalld）：**

```bash
firewall-cmd --zone=public --add-port=6379/tcp --permanent
firewall-cmd --reload
```

**Ubuntu（ufw）：**

```bash
sudo ufw allow 6379/tcp
sudo ufw reload
```

> 生产环境建议限制 IP：`ufw allow from 192.168.1.0/24 to any port 6379`

### 4.4 验证远程连接

在另一台机器上：

```bash
# 无密码
redis-cli -h 192.168.x.x -p 6379
# 有密码
redis-cli -h 192.168.x.x -p 6379 -a 123456
```

> 密码也可以登录后再输入：`AUTH 123456`

### 4.5 生产环境加固建议

```conf
# 1. 绑定内网 IP，不要用 0.0.0.0
bind 192.168.1.100

# 2. 开启保护模式
protected-mode yes

# 3. 设置强密码
requirepass xxxxxStrongPasswordxxxxx

# 4. 重命名危险命令（避免误操作或被攻击）
rename-command CONFIG ""
rename-command FLUSHALL ""
rename-command FLUSHDB ""
rename-command KEYS ""
rename-command SHUTDOWN ""

# 5. 限制客户端连接数
maxclients 1000

# 6. 开启持久化（RDB + AOF 双保险）
save 900 1
save 300 10
save 60 10000
appendonly yes
appendfsync everysec
```

---

## 五、项目配置文件示例

### 5.1 Spring Boot 后端 application.yml

文件位置：`backend/src/main/resources/application.yml`

```yaml
spring:
  data:
    redis:
      # 主机地址
      host: 127.0.0.1
      # 端口
      port: 6379
      # 密码（无密码则留空或删除该行）
      password: ""
      # 数据库索引（0-15，默认 0）
      database: 0
      # 连接超时（毫秒）
      timeout: 3000ms
      # 客户端类型
      client-type: lettuce

      # Lettuce 连接池配置
      lettuce:
        pool:
          # 最大连接数
          max-active: 8
          # 最大空闲连接
          max-idle: 8
          # 最小空闲连接
          min-idle: 0
          # 连接池最大阻塞等待时间（-1 不限制）
          max-wait: -1ms
```

**不同环境的配置示例：**

```yaml
# 开发环境（本地无密码）
spring:
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password: ""
      database: 0

# 测试环境（内网带密码）
spring:
  data:
    redis:
      host: 192.168.1.100
      port: 6379
      password: "123456"
      database: 1

# 生产环境（强密码+内网IP）
spring:
  data:
    redis:
      host: 10.0.0.50
      port: 6379
      password: "${REDIS_PASSWORD}"  # 从环境变量读取，不要写死
      database: 0
      timeout: 5000ms
      lettuce:
        pool:
          max-active: 32
          max-idle: 16
          min-idle: 4
```

### 5.2 Docker Compose 配置

```yaml
version: '3.8'

services:
  redis:
    image: redis:7.2-alpine
    container_name: qingqi-redis
    restart: always
    ports:
      - "6379:6379"
    volumes:
      - ./redis/data:/data
      - ./redis/conf/redis.conf:/etc/redis/redis.conf
    command: redis-server /etc/redis/redis.conf
    environment:
      TZ: Asia/Shanghai
```

对应 `redis.conf`（最简版）：

```conf
bind 0.0.0.0
port 6379
# requirepass 123456
databases 16
save 900 1
save 300 10
save 60 10000
dbfilename dump.rdb
dir /data
appendonly no
maxmemory 256mb
maxmemory-policy allkeys-lru
```

> 💡 Docker 方式最简单：`docker-compose up -d redis` 一键启动。

### 5.3 快速验证脚本

**Windows（test_redis.bat）：**

```batch
@echo off
echo 正在测试 Redis 连接...
redis-cli -h 127.0.0.1 -p 6379 ping
echo.
echo 写入测试数据...
redis-cli -h 127.0.0.1 -p 6379 set test:ping pong
echo 读取测试数据...
redis-cli -h 127.0.0.1 -p 6379 get test:ping
echo.
echo 删除测试数据...
redis-cli -h 127.0.0.1 -p 6379 del test:ping
echo.
echo ✅ Redis 连接正常！
pause
```

**Linux（test_redis.sh）：**

```bash
#!/bin/bash
echo "正在测试 Redis 连接..."
redis-cli -h 127.0.0.1 -p 6379 ping
echo ""
echo "写入测试数据..."
redis-cli -h 127.0.0.1 -p 6379 set test:ping pong
echo "读取测试数据..."
redis-cli -h 127.0.0.1 -p 6379 get test:ping
echo ""
echo "删除测试数据..."
redis-cli -h 127.0.0.1 -p 6379 del test:ping
echo ""
echo "✅ Redis 连接正常！"
```

---

## 六、常见报错排查

### 6.1 连接类

#### ❌ Could not connect to Redis at 127.0.0.1:6379: Connection refused

**原因：** Redis 服务未启动，或端口不对。

**排查：**
```bash
# Linux 检查进程
ps aux | grep redis

# 检查端口监听
netstat -ano | findstr 6379   # Windows
netstat -tlnp | grep 6379      # Linux
```

**解决：** 启动 Redis 服务。

#### ❌ (error) NOAUTH Authentication required

**原因：** Redis 设置了密码，但连接时没提供。

**解决：**
```bash
# 方式一：启动时带密码
redis-cli -h 127.0.0.1 -p 6379 -a 123456

# 方式二：登录后认证
redis-cli
127.0.0.1:6379> AUTH 123456
OK
```

Spring Boot 配置中加上 `password` 字段。

#### ❌ (error) DENIED Redis is running in protected mode

**原因：** 开启了保护模式，但没设密码且远程访问。

**解决：**
1. 设置密码 `requirepass xxx`
2. 或关闭保护模式 `protected-mode no`（仅内网测试）
3. 或绑定具体 IP，不监听 0.0.0.0

#### ❌ Connection reset by peer / 远程连接不上

**排查步骤：**
1. 确认 `bind 0.0.0.0` 已配置
2. 确认 `protected-mode no`（无密码时）
3. 确认防火墙已放行 6379 端口
4. 确认云服务器安全组已放行（如果是云主机）
5. `ping IP` 看网络通不通
6. `telnet IP 6379` 看端口通不通

### 6.2 配置与持久化类

#### ❌ Windows 启动闪退

**原因：** 配置文件错误或权限问题。

**排查：** 用命令行启动看报错：
```cmd
redis-server.exe redis.windows.conf
```

常见原因：
- 配置文件路径错了
- `dir` 目录不存在
- 端口被占用

#### ❌ MISCONF Redis is configured to save RDB snapshots, but is currently not able to persist on disk

**原因：** RDB 持久化失败，通常是磁盘空间不足或权限问题。

**排查：**
```bash
# 查看磁盘空间
df -h

# 查看数据目录权限
ls -ld /var/lib/redis   # CentOS
ls -ld /data/redis       # 自定义
```

**临时解决（不推荐，仅临时应急）：**
```bash
redis-cli config set stop-writes-on-bgsave-error no
```

**正确解决：** 清理磁盘空间、修正目录权限。

#### ❌ Can't open the append-only file

**原因：** AOF 文件打不开，通常是权限或磁盘问题。

**解决：** 检查 AOF 文件所在目录权限和磁盘空间。

### 6.3 内存类

#### ❌ OOM command not allowed when used memory > 'maxmemory'

**原因：** Redis 内存使用超过了 `maxmemory` 限制。

**排查：**
```bash
redis-cli info memory | grep used_memory_human
redis-cli config get maxmemory
```

**解决：**
1. 调大 `maxmemory`（如果物理内存够）
2. 开启内存淘汰策略：
   ```
   config set maxmemory-policy allkeys-lru
   ```
3. 清理没用的 key：`redis-cli --scan --pattern "xxx:*" | xargs redis-cli del`

#### ❌ 内存占用持续增长，不释放

**原因：** Redis 内存分配后不会主动归还给操作系统（正常现象）。

**排查：**
```bash
redis-cli info memory
# 关注 used_memory_rss（实际占用物理内存）和 used_memory（数据占用）
```

**解决：**
- 如果 used_memory 远小于 used_memory_rss，可以考虑内存碎片整理：
  ```bash
  redis-cli memory purge   # Redis 4.0+
  ```
- 或重启 Redis（会重新加载，内存变紧凑）

### 6.4 性能类

#### ❌ Redis 响应变慢

**排查步骤：**
```bash
# 1. 查看慢查询
redis-cli slowlog get 10

# 2. 查看 INFO 信息
redis-cli info stats
redis-cli info memory
redis-cli info cpu

# 3. 检查大 key
redis-cli --bigkeys
```

常见原因：
- 使用了 `KEYS *` 命令（生产禁用，用 `SCAN` 替代）
- 存在大 value 的 key（如几 MB 的字符串）
- 内存达到 maxmemory 后频繁淘汰
- RDB/AOF fork 子进程导致阻塞
- 网络延迟

#### ❌ 大量 TIME_WAIT 连接

**原因：** 短连接太多，连接释放后进入 TIME_WAIT。

**解决：**
1. 应用端使用连接池（Spring Boot 默认用 Lettuce 连接池）
2. 调整系统内核参数：
   ```bash
   echo "net.ipv4.tcp_tw_reuse = 1" >> /etc/sysctl.conf
   sysctl -p
   ```

### 6.5 其他常见问题

#### ❌ (error) ERR unknown command 'CONFIG'

**原因：** `CONFIG` 命令被重命名或禁用了（生产环境常见加固手段）。

**解决：** 用 redis-cli 直接修改配置文件，然后重启 Redis。

#### ❌ Redis 突然变慢且 CPU 很高

**排查：**
```bash
redis-cli info cpu
redis-cli slowlog get 20
```

常见原因：
- 有慢查询（大 key、复杂操作）
- 正在做 RDB 快照 fork 子进程
- AOF 刷盘策略是 `always`（改成 `everysec`）

#### ❌ 主从同步问题（如有主从）

如果配置了主从复制：
```bash
# 查看主从状态
redis-cli info replication

# 常见原因：
# 1. 网络不通 → 检查防火墙
# 2. 密码不一致 → 主节点密码和从节点 masterauth 一致
# 3. 全量同步超时 → 调大 repl-timeout
```

---

## 附录：部署检查清单

部署完成后，按以下清单逐项确认：

- [ ] Redis 服务已启动，开机自启已配置
- [ ] 端口 6379 可本地连接，`ping` 返回 `PONG`
- [ ] （内网测试）远程可连接，防火墙已放行
- [ ] （生产环境）已设置强密码，bind 为内网 IP
- [ ] （生产环境）危险命令已重命名（CONFIG/FLUSHALL/KEYS 等）
- [ ] 持久化已配置（RDB 或 RDB+AOF）
- [ ] 数据目录权限正确，磁盘空间充足
- [ ] maxmemory 已设置合理值，淘汰策略已配置
- [ ] 应用配置文件中 Redis 连接信息正确
- [ ] 应用启动时能正常连接 Redis，无报错

---

## 附录：Redis 常用命令速查

```bash
# 连接
redis-cli -h host -p port -a password

# 键操作
keys pattern          # 查找键（生产禁用，用 SCAN）
exists key            # 判断键是否存在
type key              # 查看键类型
del key1 key2         # 删除键
expire key seconds    # 设置过期时间
ttl key               # 查看剩余过期时间
persist key           # 移除过期时间

# 字符串
set key value
get key
incr key              # 自增
decr key              # 自减
setex key seconds value  # 设置值并指定过期时间

# Hash
hset key field value
hget key field
hgetall key
hdel key field
hlen key

# List
lpush key value
rpush key value
lpop key
rpop key
lrange key start stop
llen key

# Set
sadd key member
smembers key
srem key member
scard key

# ZSet
zadd key score member
zrange key start stop
zrevrange key start stop
zscore key member

# 服务
ping
info                  # 查看服务器信息
config get parameter  # 获取配置
dbsize                # 当前库 key 数量
flushdb               # 清空当前库（生产慎用）
flushall              # 清空所有库（生产慎用）
save                  # 手动触发 RDB 保存
bgsave                # 后台异步 RDB 保存
shutdown              # 关闭服务
```
