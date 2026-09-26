# 青启e城 · MySQL 数据库部署文档

> 适用场景：在新机器上从零部署青启e城项目数据库，支持 Windows / Linux 双平台。
> 默认账号：`root` / `123456`（仅内网测试使用，生产环境请务必修改强密码）

---

## ⚠️ 安全声明

> **`root@%` 仅用于内网测试环境，生产环境绝对不安全！**
>
> - 生产环境必须禁用 root 远程登录，创建专用业务账号并最小权限授权
> - 生产密码必须 12 位以上，包含大小写字母+数字+特殊字符
> - 生产环境建议限制 IP 白名单，而非开放 `%` 任意主机
> - 本文档中的 `123456` 为演示弱密码，**严禁用于生产**

---

## 目录

- [一、Windows 部署流程](#一windows-部署流程)
- [二、Linux 部署流程](#二linux-部署流程)
- [三、创建数据库与导入数据](#三创建数据库与导入数据)
- [四、开启远程访问（内网测试用）](#四开启远程访问内网测试用)
- [五、项目配置文件示例](#五项目配置文件示例)
- [六、常见报错排查](#六常见报错排查)

---

## 一、Windows 部署流程

### 1.1 下载 MySQL

推荐使用 MySQL 8.0.x（项目基于 8.0 开发）：

- 官方下载：<https://dev.mysql.com/downloads/mysql/>
- 选择 **Windows (x86, 64-bit), ZIP Archive** 版本（免安装版更可控）
- 或下载 MySQL Installer（图形化安装）

### 1.2 方式 A：免安装版（ZIP）部署

**Step 1：解压到目标目录**

```
解压到 D:\mysql-8.0.x\
```

**Step 2：创建配置文件**

在 MySQL 根目录下新建 `my.ini`：

```ini
[mysqld]
# 设置3306端口
port=3306
# 设置mysql的安装目录
basedir=D:\\mysql-8.0.x
# 设置mysql数据库的数据的存放目录
datadir=D:\\mysql-8.0.x\\data
# 允许最大连接数
max_connections=200
# 允许连接失败的次数
max_connect_errors=10
# 服务端使用的字符集默认为utf8mb4
character-set-server=utf8mb4
# 创建新表时将使用的默认存储引擎
default-storage-engine=INNODB
# 默认使用"mysql_native_password"插件认证
default_authentication_plugin=mysql_native_password
# 时区设置（避免时间差8小时）
default-time-zone = '+08:00'

[mysql]
# 设置mysql客户端默认字符集
default-character-set=utf8mb4

[client]
# 设置mysql客户端连接服务端时默认使用的端口
port=3306
default-character-set=utf8mb4
```

**Step 3：初始化数据库**

以**管理员身份**打开 cmd，进入 MySQL 的 bin 目录：

```cmd
cd D:\mysql-8.0.x\bin
mysqld --initialize --console
```

执行完成后，会输出 root 用户的初始默认密码，形如：

```
[Note] [MY-010454] [Server] A temporary password is generated for root@localhost: xxxxxxx
```

> 📝 把这个临时密码记下来，后面要用。如果没看到，去 `data/` 目录下找 `.err` 后缀的文件，里面也有。

**Step 4：安装 MySQL 服务**

```cmd
mysqld --install mysql
```

看到 `Service successfully installed.` 即为成功。

**Step 5：启动 MySQL 服务**

```cmd
net start mysql
```

**Step 6：修改 root 密码**

```cmd
mysql -u root -p
```

输入刚才记下的临时密码，登录后执行：

```sql
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '123456';
FLUSH PRIVILEGES;
```

> ⚠️ `123456` 为测试环境弱密码，生产环境请修改为强密码。

### 1.3 方式 B：Installer 图形化安装

1. 运行 MySQL Installer，选择 **Server only** 或 **Full**
2. 一路 Next，设置 root 密码为 `123456`（测试用）
3. 端口保持默认 `3306`
4. 勾选 "Configure MySQL Server as a Windows Service"
5. 完成安装

### 1.4 验证安装

```cmd
mysql -u root -p123456 -e "SELECT VERSION();"
```

能输出版本号即安装成功。

---

## 二、Linux 部署流程

以下以 **CentOS 7/8** 和 **Ubuntu 20.04/22.04** 为例。

### 2.1 CentOS 7 / RockyLinux / AlmaLinux

**Step 1：添加 MySQL Yum 仓库**

```bash
# 下载 RPM 包（版本号以官网最新为准）
wget https://dev.mysql.com/get/mysql80-community-release-el7-7.noarch.rpm
# 安装仓库
rpm -ivh mysql80-community-release-el7-7.noarch.rpm
```

> CentOS 8 用 `el8` 版本的包。

**Step 2：安装 MySQL Server**

```bash
yum install -y mysql-community-server
```

如果报 GPG 密钥错误：

```bash
rpm --import https://repo.mysql.com/RPM-GPG-KEY-mysql-2023
yum install -y mysql-community-server
```

**Step 3：启动并设置开机自启**

```bash
systemctl start mysqld
systemctl enable mysqld
systemctl status mysqld
```

**Step 4：获取临时密码并修改**

```bash
# 查看临时密码
grep 'temporary password' /var/log/mysqld.log

# 登录
mysql -u root -p
```

登录后修改密码：

```sql
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '123456';
FLUSH PRIVILEGES;
```

> 如果提示密码太简单不符合策略，先设置一个复杂密码，再修改密码策略（仅限测试环境）：
> ```sql
> SET GLOBAL validate_password.policy = 0;
> SET GLOBAL validate_password.length = 4;
> ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '123456';
> ```

### 2.2 Ubuntu 20.04 / 22.04

**Step 1：安装 MySQL**

```bash
sudo apt update
sudo apt install -y mysql-server
```

**Step 2：启动服务**

```bash
sudo systemctl start mysql
sudo systemctl enable mysql
```

**Step 3：初始化安全配置**

```bash
sudo mysql_secure_installation
```

按提示操作，设置 root 密码为 `123456`（测试用）。

**Step 4：修改认证方式（如果用 mysql_native_password）**

```bash
sudo mysql -u root
```

```sql
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '123456';
FLUSH PRIVILEGES;
exit;
```

### 2.3 Linux 验证安装

```bash
mysql -u root -p123456 -e "SELECT VERSION();"
```

---

## 三、创建数据库与导入数据

### 3.1 创建数据库

登录 MySQL：

```bash
mysql -u root -p123456
```

执行建库语句：

```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS qingqi
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

-- 查看是否创建成功
SHOW DATABASES LIKE 'qingqi';
```

### 3.2 导入建表脚本（schema.sql）

**Windows：**

```cmd
cd D:\codex\codex-data\qingqi-ecity\backend\sql
mysql -u root -p123456 qingqi < schema.sql
```

**Linux：**

```bash
cd /path/to/qingqi-ecity/backend/sql
mysql -u root -p123456 qingqi < schema.sql
```

验证表数量：

```sql
USE qingqi;
SHOW TABLES;
-- 正常应显示 25 张表
SELECT COUNT(*) AS table_count FROM information_schema.tables WHERE table_schema = 'qingqi';
```

### 3.3 导入演示数据（data.sql）

```bash
mysql -u root -p123456 qingqi < data.sql
```

> data.sql 使用 `INSERT IGNORE`，可重复导入不会报错。

验证数据：

```sql
SELECT COUNT(*) FROM sys_user;     -- 应返回 5（5个演示账号）
SELECT COUNT(*) FROM sys_role;     -- 应返回 4（4个角色）
SELECT * FROM sys_user LIMIT 5;    -- 查看用户列表
```

### 3.4 一键导入（推荐）

**Windows 批处理（import_db.bat）：**

```batch
@echo off
set MYSQL_HOST=127.0.0.1
set MYSQL_PORT=3306
set MYSQL_USER=root
set MYSQL_PASS=123456
set DB_NAME=qingqi

echo [1/3] 创建数据库 %DB_NAME%...
mysql -h%MYSQL_HOST% -P%MYSQL_PORT% -u%MYSQL_USER% -p%MYSQL_PASS% -e "CREATE DATABASE IF NOT EXISTS %DB_NAME% DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_general_ci;"

echo [2/3] 导入表结构 schema.sql...
mysql -h%MYSQL_HOST% -P%MYSQL_PORT% -u%MYSQL_USER% -p%MYSQL_PASS% %DB_NAME% < schema.sql

echo [3/3] 导入演示数据 data.sql...
mysql -h%MYSQL_HOST% -P%MYSQL_PORT% -u%MYSQL_USER% -p%MYSQL_PASS% %DB_NAME% < data.sql

echo.
echo ✅ 数据库导入完成！
echo.
pause
```

**Linux Shell 脚本（import_db.sh）：**

```bash
#!/bin/bash
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3306
MYSQL_USER=root
MYSQL_PASS=123456
DB_NAME=qingqi

echo "[1/3] 创建数据库 ${DB_NAME}..."
mysql -h${MYSQL_HOST} -P${MYSQL_PORT} -u${MYSQL_USER} -p${MYSQL_PASS} -e "CREATE DATABASE IF NOT EXISTS ${DB_NAME} DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_general_ci;"

echo "[2/3] 导入表结构 schema.sql..."
mysql -h${MYSQL_HOST} -P${MYSQL_PORT} -u${MYSQL_USER} -p${MYSQL_PASS} ${DB_NAME} < schema.sql

echo "[3/3] 导入演示数据 data.sql..."
mysql -h${MYSQL_HOST} -P${MYSQL_PORT} -u${MYSQL_USER} -p${MYSQL_PASS} ${DB_NAME} < data.sql

echo ""
echo "✅ 数据库导入完成！"
```

---

## 四、开启远程访问（内网测试用）

> ⚠️ **重要安全提示**
> - 以下操作**仅适用于内网测试/开发环境**
> - 生产环境严禁使用 `root@%`，应创建专用账号并限制 IP
> - 生产环境建议配合防火墙只放行指定 IP 段

### 4.1 创建 root 远程访问账号

```sql
-- 允许 root 从任意 IP 访问（仅内网测试用！）
CREATE USER 'root'@'%' IDENTIFIED WITH mysql_native_password BY '123456';

-- 授予全部权限
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;

-- 刷新权限
FLUSH PRIVILEGES;
```

### 4.2 修改 MySQL 绑定地址

默认 MySQL 只监听 127.0.0.1，需要改为 0.0.0.0 才能远程访问。

**Windows：**

编辑 `my.ini`，在 `[mysqld]` 段添加或修改：

```ini
bind-address = 0.0.0.0
```

重启 MySQL 服务：

```cmd
net stop mysql
net start mysql
```

**Linux（CentOS/Ubuntu）：**

编辑配置文件（CentOS 路径）：

```bash
vi /etc/my.cnf
```

或（Ubuntu 路径）：

```bash
vi /etc/mysql/mysql.conf.d/mysqld.cnf
```

在 `[mysqld]` 段添加/修改：

```ini
bind-address = 0.0.0.0
```

重启服务：

```bash
# CentOS
systemctl restart mysqld

# Ubuntu
systemctl restart mysql
```

### 4.3 防火墙放行 3306 端口

**Windows 防火墙：**

```powershell
# 以管理员身份运行 PowerShell
New-NetFirewallRule -DisplayName "MySQL 3306" -Direction Inbound -Protocol TCP -LocalPort 3306 -Action Allow
```

或图形化操作：Windows  Defender 防火墙 → 高级设置 → 入站规则 → 新建规则 → 端口 → TCP 3306 → 允许连接

**CentOS（firewalld）：**

```bash
firewall-cmd --zone=public --add-port=3306/tcp --permanent
firewall-cmd --reload
firewall-cmd --list-ports
```

**Ubuntu（ufw）：**

```bash
ufw allow 3306/tcp
ufw reload
ufw status
```

> 生产环境建议：`ufw allow from 192.168.1.0/24 to any port 3306`（仅允许内网网段）

### 4.4 验证远程连接

在另一台机器上测试：

```bash
mysql -h 192.168.x.x -P 3306 -u root -p123456
```

能连上即配置成功。

---

## 五、项目配置文件示例

### 5.1 Spring Boot 后端 application.yml

文件位置：`backend/src/main/resources/application.yml`

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    # 本地数据库
    url: jdbc:mysql://127.0.0.1:3306/qingqi?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: "123456"

    # 远程数据库（内网测试，替换为实际IP）
    # url: jdbc:mysql://192.168.1.100:3306/qingqi?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    # username: root
    # password: "123456"

  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password: ""
      database: 0

mybatis-plus:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.icbc.qingqi.module.**.entity
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

### 5.2 Docker Compose 配置

文件位置：`docker-compose.yml`（数据库部分）

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: qingqi-mysql
    restart: always
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: "123456"
      MYSQL_DATABASE: qingqi
      TZ: Asia/Shanghai
    volumes:
      - ./mysql/data:/var/lib/mysql
      - ./mysql/conf/my.cnf:/etc/mysql/conf.d/my.cnf
      # 自动初始化：容器首次启动会自动执行 /docker-entrypoint-initdb.d/ 下的 SQL
      - ./backend/sql/schema.sql:/docker-entrypoint-initdb.d/01_schema.sql:ro
      - ./backend/sql/data.sql:/docker-entrypoint-initdb.d/02_data.sql:ro
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_general_ci
      - --default-authentication-plugin=mysql_native_password
      - --bind-address=0.0.0.0
```

> 💡 Docker 方式最简单：`docker-compose up -d` 即可一键启动 MySQL 并自动导入数据。

---

## 六、常见报错排查

### 6.1 连接类

#### ❌ ERROR 2003 (HY000): Can't connect to MySQL server

**原因：** MySQL 服务未启动，或端口/地址不对。

**排查：**
```bash
# Windows 检查服务
sc query mysql

# Linux 检查服务
systemctl status mysqld

# 检查端口监听
netstat -ano | findstr 3306   # Windows
netstat -tlnp | grep 3306      # Linux
```

**解决：** 启动 MySQL 服务，确认端口为 3306。

#### ❌ Access denied for user 'root'@'localhost'

**原因：** 密码错误，或用户不存在。

**排查：**
```sql
SELECT user, host FROM mysql.user;
```

**解决：**
- Windows 免安装版：跳过权限认证重置密码
  ```cmd
  # 停止服务
  net stop mysql
  # 以跳过权限方式启动
  mysqld --console --skip-grant-tables --shared-memory
  # 另开一个 cmd
  mysql -u root
  FLUSH PRIVILEGES;
  ALTER USER 'root'@'localhost' IDENTIFIED BY '123456';
  FLUSH PRIVILEGES;
  ```
- Linux：
  ```bash
  # 编辑配置，在 [mysqld] 下添加 skip-grant-tables
  vi /etc/my.cnf
  # 重启
  systemctl restart mysqld
  mysql -u root
  FLUSH PRIVILEGES;
  ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '123456';
  FLUSH PRIVILEGES;
  # 改完把 skip-grant-tables 去掉，再重启
  ```

#### ❌ Host 'xxx' is not allowed to connect to this MySQL server

**原因：** 远程访问未开启，账号的 `host` 字段不是 `%`。

**解决：**
```sql
-- 检查现有账号
SELECT user, host FROM mysql.user;

-- 创建远程访问账号（参考第 4 节）
CREATE USER 'root'@'%' IDENTIFIED WITH mysql_native_password BY '123456';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;
FLUSH PRIVILEGES;
```

同时确认 `bind-address = 0.0.0.0` 和防火墙已放行。

#### ❌ Public Key Retrieval is not allowed

**原因：** MySQL 8.0 默认 `caching_sha2_password` 认证，JDBC 连接时需要获取公钥。

**解决：** JDBC URL 加上 `allowPublicKeyRetrieval=true`：

```
jdbc:mysql://127.0.0.1:3306/qingqi?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=Asia/Shanghai
```

或修改用户认证方式为 `mysql_native_password`：
```sql
ALTER USER 'root'@'%' IDENTIFIED WITH mysql_native_password BY '123456';
FLUSH PRIVILEGES;
```

### 6.2 导入 SQL 类

#### ❌ ERROR 1046 (3D000): No database selected

**原因：** 没指定数据库名。

**解决：** 命令行加数据库名：
```bash
mysql -u root -p123456 qingqi < schema.sql
```

或 SQL 开头加 `USE qingqi;`。

#### ❌ ERROR 1050 (42S01): Table 'xxx' already exists

**原因：** 表已经存在，重复导入。

**解决：**
- schema.sql 不建议重复导入（会覆盖数据）
- data.sql 使用了 `INSERT IGNORE`，可以重复导入
- 如果需要完全重置：
  ```sql
  DROP DATABASE qingqi;
  CREATE DATABASE qingqi ...;
  -- 然后重新导入
  ```

#### ❌ ERROR 1064 (42000): You have an error in your SQL syntax

**原因：** SQL 语法错误，常见于版本不兼容或特殊字符。

**排查：** 记下报错的行号，去 SQL 文件里找到对应行检查。

常见原因：
- MySQL 5.x 导入 MySQL 8.0 的 SQL（部分语法不兼容）
- 文件编码问题（必须是 UTF-8）
- 中文注释引起的编码问题

#### ❌ ERROR 1366 (HY000): Incorrect string value

**原因：** 字符集不匹配，数据库/表不是 utf8mb4。

**解决：**
```sql
-- 修改数据库字符集
ALTER DATABASE qingqi CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- 修改表字符集
ALTER TABLE 表名 CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

### 6.3 应用连接类

#### ❌ Communications link failure / 连接超时

**原因：** 网络不通、防火墙拦截、MySQL 没监听远程地址。

**排查步骤：**
1. 应用服务器 ping 数据库服务器 IP
2. 应用服务器 telnet IP 3306（测试端口）
3. 检查 MySQL `bind-address` 配置
4. 检查防火墙/安全组

#### ❌ The server time zone value 'xxx' is unrecognized

**原因：** 时区不匹配。

**解决：** JDBC URL 加 `serverTimezone=Asia/Shanghai`：
```
jdbc:mysql://127.0.0.1:3306/qingqi?serverTimezone=Asia/Shanghai
```

或修改 MySQL 全局配置：
```sql
SET GLOBAL time_zone = '+08:00';
```

#### ❌ Unknown database 'qingqi'

**原因：** 数据库还没创建。

**解决：** 先执行建库语句（见 3.1 节）。

### 6.4 性能与其他

#### ❌ 导入 SQL 很慢

**原因：** 数据量大或 autocommit 逐条提交。

**优化：** 在 SQL 文件开头加上：
```sql
SET autocommit = 0;
SET FOREIGN_KEY_CHECKS = 0;
```
结尾加上：
```sql
SET FOREIGN_KEY_CHECKS = 1;
COMMIT;
SET autocommit = 1;
```

#### ❌ 表名大小写问题（Linux）

**原因：** Linux 下 MySQL 默认区分表名大小写，Windows 不区分。

**解决：** 在 `my.cnf` 的 `[mysqld]` 段添加：
```ini
lower_case_table_names = 1
```
> 注意：这个参数必须在初始化数据库之前设置，已经有数据的库修改会导致问题。

---

## 附录：部署检查清单

部署完成后，按以下清单逐项确认：

- [ ] MySQL 服务已启动，开机自启已配置
- [ ] root 密码已设置（测试用 123456，生产环境请改强密码）
- [ ] 数据库 `qingqi` 已创建，字符集 utf8mb4
- [ ] schema.sql 已导入，表数量 = 25
- [ ] data.sql 已导入，5 个演示账号可登录
- [ ] （如需远程访问）bind-address = 0.0.0.0 已配置
- [ ] （如需远程访问）防火墙 3306 端口已放行
- [ ] （如需远程访问）已创建 `root@%` 远程账号
- [ ] 应用配置文件中数据库连接信息正确
- [ ] ⚠️ 生产环境已禁用 root 远程访问，创建了专用业务账号
