# MySQL 部署文档

> 本文档覆盖 MySQL 8.0 的安装、初始化、远程访问配置与常见问题，支持 Windows / Linux 双平台。
> 项目默认账号：`root / 123456`，数据库名：`qingqi`，字符集：`utf8mb4`。

---

## 目录

- [一、Windows 部署](#一windows-部署)
- [二、Linux 部署](#二linux-部署)
- [三、创建数据库与导入数据](#三创建数据库与导入数据)
- [四、开启远程访问](#四开启远程访问)
- [五、项目配置示例](#五项目配置示例)
- [六、常见问题排查](#六常见问题排查)

---

## 一、Windows 部署

### 方式 A：ZIP 免安装版（推荐）

1. 下载 MySQL 8.0 ZIP 版：<https://dev.mysql.com/downloads/mysql/>
2. 解压到 `D:\mysql-8.0.x\`
3. 在目录下创建 `my.ini`：

```ini
[mysqld]
basedir=D:/mysql-8.0.x
datadir=D:/mysql-8.0.x/data
port=3306
character-set-server=utf8mb4
default-storage-engine=INNODB
```

4. 以**管理员身份**打开 cmd，初始化并安装服务：

```cmd
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

### 方式 B：Installer 图形化安装

1. 下载 Installer：<https://dev.mysql.com/downloads/installer/>
2. 选择 MySQL Server 8.0 + Workbench，一路 Next
3. 设置 root 密码时**设为 `123456`**（与项目默认配置一致）
4. 完成安装

---

## 二、Linux 部署

### CentOS 7 / RockyLinux / AlmaLinux

```bash
# 安装官方仓库
wget https://dev.mysql.com/get/mysql80-community-release-el7-7.noarch.rpm
rpm -ivh mysql80-community-release-el7-7.noarch.rpm
yum install -y mysql-community-server

# 启动并设置开机自启
systemctl start mysqld
systemctl enable mysqld

# 查看临时密码并修改为 123456
grep 'temporary password' /var/log/mysqld.log
mysql -uroot -p        # 输入上面查到的临时密码
ALTER USER 'root'@'localhost' IDENTIFIED BY '123456';
FLUSH PRIVILEGES;
EXIT;
```

### Ubuntu 20.04 / 22.04

```bash
sudo apt update
sudo apt install -y mysql-server

# 启动并设置开机自启
sudo systemctl start mysql
sudo systemctl enable mysql

# 设置 root 密码
sudo mysql
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '123456';
FLUSH PRIVILEGES;
EXIT;
```

---

## 三、创建数据库与导入数据

### 手动导入

```bash
# 1. 创建数据库
mysql -u root -p123456 -e "CREATE DATABASE IF NOT EXISTS qingqi DEFAULT CHARSET utf8mb4 DEFAULT COLLATE utf8mb4_general_ci;"

# 2. 导入表结构（41 张表）
mysql -u root -p123456 qingqi < backend/sql/schema.sql

# 3. 导入演示数据
mysql -u root -p123456 qingqi < backend/sql/data.sql
```

> SQL 文件位置：`backend/sql/`（唯一权威版本）。
> data.sql 内置 5 个演示账号（admin / testuser / entrepreneur / landlord01 / banker01），统一密码 `123456`。

### 脚本一键导入（推荐）

**Windows：**
```cmd
cd scripts\windows
01-init-db.bat
```

**Linux：**
```bash
cd scripts/linux
chmod +x *.sh
./01-init-db.sh
```

脚本会自动完成：创建数据库 → 导入表结构 → 导入演示数据 → 验证表数量和账号数。

### 验证

```sql
USE qingqi;
SELECT COUNT(*) AS table_count FROM information_schema.tables WHERE table_schema='qingqi';
-- 应为 41

SELECT username, role FROM sys_user;
-- 应返回 5 条演示账号
```

---

## 四、开启远程访问

> ⚠️ **安全警告**：以下操作仅适用于内网开发测试环境。生产环境禁止使用 `root@'%'`，应创建专用业务账号并限制 IP 白名单。

### 第 1 步：创建允许远程登录的账号

```sql
-- 创建 root@'%'（仅内网测试用！）
CREATE USER IF NOT EXISTS 'root'@'%' IDENTIFIED BY '123456';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;
FLUSH PRIVILEGES;
```

### 第 2 步：修改 bind-address

**Windows** — 在 `my.ini` 的 `[mysqld]` 下添加：
```ini
bind-address=0.0.0.0
```
然后重启 MySQL 服务：`net stop mysql && net start mysql`

**Linux（CentOS/Ubuntu）** — 编辑 `/etc/my.cnf` 或 `/etc/mysql/mysql.conf.d/mysqld.cnf`：
```ini
bind-address=0.0.0.0
```
然后重启：`systemctl restart mysqld`（或 `mysql`）

### 第 3 步：防火墙放行 3306 端口

**Windows — 直接关闭防火墙或添加入站规则（内网开发可简化）。**

**CentOS：**
```bash
firewall-cmd --zone=public --add-port=3306/tcp --permanent
firewall-cmd --reload
```

**Ubuntu：**
```bash
sudo ufw allow 3306/tcp
sudo ufw reload
```

### 生产环境正确做法

```sql
-- 创建专用业务账号，限制只能从指定 IP 访问
CREATE USER 'qingqi_app'@'192.168.1.%' IDENTIFIED BY '强密码';
GRANT SELECT, INSERT, UPDATE, DELETE ON qingqi.* TO 'qingqi_app'@'192.168.1.%';
FLUSH PRIVILEGES;
```

---

## 五、项目配置示例

### Spring Boot application.yml

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/qingqi?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: 123456
    driver-class-name: com.mysql.cj.jdbc.Driver
```

> 密码不同时，通过环境变量 `SPRING_DATASOURCE_PASSWORD` 注入，或直接修改 `application.yml`。

### Docker Compose 方式

项目根目录 `docker-compose.yml` 已配置 MySQL 服务，一键启动：

```bash
docker compose up -d mysql
```

- 端口：`3307`（映射到宿主机；容器内为 3306，注意与 docker-compose.md 保持一致）
- root 密码：`123456`
- 数据库：`qingqi`（自动创建，自动导入 schema.sql + data.sql）

---

## 六、常见问题排查

| 报错信息 | 原因 | 解决 |
|----------|------|------|
| `Access denied for user 'root'@'localhost'` | 密码错误 | 确认 MySQL root 密码是否为 `123456`，不一致则修改密码或改 application.yml |
| `Communications link failure` | MySQL 未启动 / 端口不对 / 防火墙 | 检查服务状态、端口 3306、防火墙放行 |
| `Unknown database 'qingqi'` | 数据库未创建 | 执行 `CREATE DATABASE qingqi` 或重新运行 01-init-db 脚本 |
| 表不存在 / Table doesn't exist | schema.sql 未导入 | 重新导入 schema.sql，确认导入到了 `qingqi` 库 |
| 中文乱码 / Incorrect string value | 字符集不是 utf8mb4 | 数据库/表/连接字符集都必须是 utf8mb4；JDBC URL 加 `characterEncoding=utf8` |
| `Can't connect to MySQL server on 'xxx'` | 远程访问不通 | 检查 bind-address、防火墙、账号 host 是否为 `%` 或对应 IP |
| `The server time zone value` 报错 | 时区问题 | JDBC URL 加上 `serverTimezone=Asia/Shanghai` |
| `Public Key Retrieval is not allowed` | 缓存 SHA2 密码插件 | JDBC URL 加上 `allowPublicKeyRetrieval=true` |
| 导入 data.sql 报错 "重复主键" | 重复导入 | data.sql 使用 INSERT IGNORE，重复导入不会报错；如遇报错检查是否手动改过数据 |

> 更多问题见 [05-troubleshooting.md](./05-troubleshooting.md)。
