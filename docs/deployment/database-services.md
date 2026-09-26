# 青启e城 · 数据库服务安装参考（MySQL + Redis）

> 本文档属于「本地部署与配置指南」系列，完整目录见 [README.md](./README.md)
> 覆盖 MySQL 8.0 与 Redis 6.0+ 的安装、启动、初始化与常见问题，支持 Windows / Linux 双平台。

---

## 目录

- [一、MySQL 部署](#一mysql-部署)
- [二、Redis 部署](#二redis-部署)
- [三、Docker 方式启动数据库](#三docker-方式启动数据库)
- [附录：部署检查清单](#附录部署检查清单)

---

## 一、MySQL 部署

### 1.1 Windows 安装

**方式 A：ZIP 免安装版（推荐）**

1. 下载 MySQL 8.0 ZIP 版：<https://dev.mysql.com/downloads/mysql/>
2. 解压到 `D:\mysql-8.0.x\`
3. 在目录下创建 `my.ini`，关键配置：

```ini
[mysqld]
basedir=D:/mysql-8.0.x
datadir=D:/mysql-8.0.x/data
port=3306
character-set-server=utf8mb4
default-storage-engine=INNODB
```

4. 初始化并安装服务：

```cmd
# 以管理员身份运行 cmd
cd /d D:\mysql-8.0.x\bin
mysqld --initialize-insecure        # 初始化（root 无密码）
mysqld --install mysql              # 注册 Windows 服务
net start mysql                     # 启动服务
```

5. 设置 root 密码为 `123456`（与项目默认配置一致）：

```cmd
mysql -u root -p --skip-password
ALTER USER 'root'@'localhost' IDENTIFIED BY '123456';
FLUSH PRIVILEGES;
EXIT;
```

**方式 B：Installer 图形化安装**

1. 下载 Installer：<https://dev.mysql.com/downloads/installer/>
2. 一路 Next，选择 MySQL Server 8.0 + Workbench
3. 设置 root 密码时**设为 `123456`**（与项目默认配置一致，否则需改 application.yml）
4. 完成安装

### 1.2 Linux 安装

**CentOS 7 / RockyLinux / AlmaLinux：**

```bash
# 下载并安装官方仓库（版本号以官网最新为准）
wget https://dev.mysql.com/get/mysql80-community-release-el7-7.noarch.rpm
rpm -ivh mysql80-community-release-el7-7.noarch.rpm
yum install -y mysql-community-server

# 启动并设置开机自启
systemctl start mysqld
systemctl enable mysqld

# 查看临时密码并修改为 123456
grep 'temporary password' /var/log/mysqld.log
mysql -uroot -p        # 输入临时密码
ALTER USER 'root'@'localhost' IDENTIFIED BY '123456';
FLUSH PRIVILEGES;
EXIT;
```

**Ubuntu 20.04 / 22.04：**

```bash
sudo apt update
sudo apt install -y mysql-server

# 启动并设置开机自启
sudo systemctl start mysql
sudo systemctl enable mysql

# 进入 MySQL 设置 root 密码（或使用 sudo mysql 免密进入）
sudo mysql
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '123456';
FLUSH PRIVILEGES;
EXIT;
```

### 1.3 创建数据库与导入数据

```cmd
# 创建数据库
mysql -u root -p123456 -e "CREATE DATABASE IF NOT EXISTS qingqi DEFAULT CHARSET utf8mb4;"

# 导入表结构（25 张表）+ 演示数据
mysql -u root -p123456 qingqi < backend/sql/schema.sql
mysql -u root -p123456 qingqi < backend/sql/data.sql
```

> SQL 文件位置：`backend/sql/`（另有 `backend/src/main/resources/db/` 副本，内容一致）。
> data.sql 内置 5 个演示账号（admin / testuser / entrepreneur / landlord01 / banker01），统一密码 `123456`，密码为 BCrypt 哈希，可直接登录。

**验证：**

```sql
USE qingqi;
SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='qingqi';  -- 应为 25
SELECT id, username, role FROM sys_user LIMIT 5;                              -- 应为 5 条
```

> 也可以直接运行脚本：Windows `docs\scripts\windows\01-init-db.bat`，Linux `docs/scripts/linux/01-init-db.sh`。

### 1.4 常见问题

| 问题 | 解决 |
|------|------|
| `Access denied for user 'root'` | 密码与 application.yml 不一致：要么把 MySQL 密码改为 `123456`，要么修改 application.yml 的 `spring.datasource.password` |
| `Communications link failure` | MySQL 服务未启动 / 端口不是 3306 / 防火墙拦截 |
| 表不存在 | schema.sql 未导入或导错库，重新导入 |
| 中文乱码 | 数据库字符集必须是 utf8mb4；JDBC URL 需含 `characterEncoding=utf8` |
| 远程连接不上 | 创建 `root@'%'` 账号 + `bind-address=0.0.0.0` + 防火墙放行 3306 |

> 更多排查见 [05-troubleshooting.md](./05-troubleshooting.md#二数据库类问题)。

---

## 二、Redis 部署

> Redis 用于 **JWT 黑名单** 与缓存，**推荐安装**。未安装时后端自动降级（跳过黑名单检查，仅影响登出后 Token 立即失效功能）。

### 2.1 Windows 安装

1. 下载 Redis for Windows（推荐 7.x）：<https://github.com/tporadowski/redis/releases>
2. 解压到 `D:\redis\`
3. 修改 `redis.windows.conf`（开发环境保持默认即可，无需密码）：

```conf
# 端口
port 6379
# 绑定地址（本地默认 127.0.0.1）
bind 127.0.0.1
# 如需设置密码（生产/内网带密码时），取消注释并修改：
# requirepass yourpassword
```

4. 启动与注册服务：

```cmd
# 前台启动（调试用）
redis-server.exe redis.windows.conf

# 或注册为 Windows 服务（后台运行）
redis-server.exe --service-install redis.windows.conf --service-name redis
net start redis
```

5. 验证：

```cmd
redis-cli ping
# 返回 PONG 即正常
```

### 2.2 Linux 安装

**CentOS 7 / RockyLinux（EPEL 源）：**

```bash
yum install -y epel-release
yum install -y redis
systemctl start redis
systemctl enable redis
```

**Ubuntu 20.04 / 22.04：**

```bash
sudo apt update
sudo apt install -y redis-server
sudo systemctl start redis-server
sudo systemctl enable redis-server
```

**验证：**

```bash
redis-cli ping
# 返回 PONG 即正常
redis-cli info server | grep redis_version
```

### 2.3 项目连接配置

本地开发 Redis 默认**无密码**，application.yml 中保持 `password: ${SPRING_DATA_REDIS_PASSWORD:}` 即可。

如果给 Redis 设置了密码，同步修改 `backend/src/main/resources/application.yml`：

```yaml
spring:
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password: "你的Redis密码"
```

### 2.4 常见问题

| 问题 | 解决 |
|------|------|
| `Unable to connect to Redis` | Redis 未启动 / 端口不是 6379 / bind 限制 |
| `NOAUTH Authentication required` | Redis 设了密码但配置没填 / 填错，见 2.3 |
| `DENIED Redis is running in protected mode` | 远程访问且无密码：设置密码（推荐）或 `protected-mode no`（仅内网） |
| 后端打印"Redis 不可用，跳过 Token 黑名单检查" | 属降级警告，不影响登录；安装 Redis 后重启后端即可恢复黑名单功能 |

> 更多排查见 [05-troubleshooting.md](./05-troubleshooting.md#三redis-类问题)。

---

## 三、Docker 方式启动数据库

如果使用 Docker Compose 一键启动（含数据库），无需手动安装 MySQL / Redis：

```bash
docker compose up -d --build
```

- MySQL：宿主端口 **3307**（容器内 3306），root 密码 `root123456`，业务账号 `qingqi / qingqi123`
- Redis：宿主端口 **6380**（容器内 6379），密码 `redis123`

> 端口、账号以根目录 `docker-compose.yml` 为准，详细说明见 [docker-compose.md](./docker-compose.md)。

---

## 附录：部署检查清单

- [ ] MySQL 已启动，`mysql -u root -p123456 -e "SELECT VERSION();"` 正常
- [ ] 数据库 `qingqi` 已创建，字符集 utf8mb4
- [ ] schema.sql 已导入，表数量 = 25
- [ ] data.sql 已导入，5 个演示账号可用 `123456` 登录
- [ ] Redis 已启动，`redis-cli ping` 返回 PONG（推荐安装；未安装时后端自动降级）
- [ ] 后端 application.yml 中数据库密码与本地 MySQL 一致
