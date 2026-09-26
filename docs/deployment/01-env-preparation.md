# 青启e城 · 环境准备指南

> 本文档属于「本地部署与配置指南」系列，完整目录见 [README.md](./README.md)

> 适用场景：在新机器上搭建青启e城项目开发环境，支持 Windows / Linux 双平台。
> 请按顺序安装以下依赖，确保版本符合要求。

---

## 目录

- [一、前置依赖清单](#一前置依赖清单)
- [二、Windows 环境搭建](#二windows-环境搭建)
- [三、Linux 环境搭建](#三linux-环境搭建)
- [四、环境验证命令汇总](#四环境验证命令汇总)
- [五、国内镜像加速配置](#五国内镜像加速配置)

---

## 一、前置依赖清单

| 软件 | 版本要求 | 用途 | 是否必须 |
|------|----------|------|----------|
| **JDK** | 17+ | 后端 Spring Boot 运行环境 | ✅ 必须 |
| **Maven** | 3.6+ | 后端项目构建与依赖管理 | ⭕ 推荐（可用 IDE 内置或 Maven Wrapper） |
| **Node.js** | 18+ | 前端 Vue 3 开发与构建环境 | ✅ 必须 |
| **npm / pnpm** | npm 9+（随 Node） | 前端包管理 | ✅ 必须（随 Node 自带） |
| **MySQL** | 8.0 | 项目数据库 | ✅ 必须 |
| **Redis** | 6.0+ | 缓存服务（Token 黑名单、验证码等） | ⭕ 可选（MVP 阶段可不启用） |
| **Git** | 最新版 | 代码版本管理 | ✅ 必须 |
| **IDE** | IDEA / VS Code | 代码开发工具 | ⭕ 推荐 |

---

## 二、Windows 环境搭建

### 2.1 JDK 17+

**下载地址：**
- Oracle JDK：<https://www.oracle.com/java/technologies/downloads/#java17>
- OpenJDK（推荐，免费）：<https://adoptium.net/temurin/releases/?version=17>

**安装步骤：**

1. 下载 Windows x64 Installer（.msi）
2. 双击运行安装，建议安装到 `D:\Java\jdk-17\`（路径不要有空格和中文）
3. 配置环境变量：
   - 新建系统变量 `JAVA_HOME`，值为 `D:\Java\jdk-17`
   - 编辑系统变量 `Path`，添加 `%JAVA_HOME%\bin`
4. 验证：打开 cmd 执行 `java -version`

> 💡 推荐使用 **Temurin（Eclipse Adoptium）** 版本，开源免费，长期支持。

### 2.2 Maven（可选）

> 如果使用 IntelliJ IDEA，内置了 Maven，可以跳过此步。项目根目录也提供了 Maven Wrapper（`mvnw.cmd`）。

**下载地址：** <https://maven.apache.org/download.cgi>

选择 `Binary zip archive` 版本。

**安装步骤：**

1. 解压到 `D:\apache-maven-3.9.x\`
2. 配置环境变量：
   - 新建系统变量 `MAVEN_HOME`，值为 `D:\apache-maven-3.9.x`
   - 编辑 `Path`，添加 `%MAVEN_HOME%\bin`
3. 验证：`mvn -v`

### 2.3 Node.js 18+

**下载地址：** <https://nodejs.org/zh-cn/download>

选择 **LTS（长期支持版）**，Windows Installer（.msi）x64 版本。

**安装步骤：**

1. 双击 .msi 安装，一路 Next（建议安装到 `D:\nodejs\`）
2. 安装时勾选 "Automatically install the necessary tools"（可选）
3. 验证：打开 cmd 执行 `node -v` 和 `npm -v`

> 💡 Node.js 安装包已自带 npm，无需单独安装。

### 2.4 MySQL 8.0

详见 [mysql-deployment.md](./mysql-deployment.md) 第一章。

简要步骤：
1. 下载 MySQL 8.0 ZIP 版或 Installer 版
2. 配置 `my.ini`（字符集 utf8mb4、端口 3306）
3. 初始化数据库，设置 root 密码为 `123456`（测试用）
4. 启动 MySQL 服务

### 2.5 Redis（可选）

详见 [redis-deployment.md](./redis-deployment.md) 第二章。

简要步骤：
1. 下载 Redis for Windows（<https://github.com/tporadowski/redis/releases>）
2. 解压后修改 `redis.windows.conf`
3. 启动 `redis-server.exe redis.windows.conf`

### 2.6 Git

**下载地址：** <https://git-scm.com/download/win>

**安装步骤：**

1. 双击安装，一路 Next（默认配置即可）
2. 验证：打开 cmd 执行 `git --version`

**配置用户信息（首次使用）：**

```bash
git config --global user.name "你的名字"
git config --global user.email "你的邮箱"
```

### 2.7 IDE 推荐

- **后端开发**：IntelliJ IDEA Community / Ultimate
  - 下载：<https://www.jetbrains.com/idea/download/>
- **前端开发**：Visual Studio Code
  - 下载：<https://code.visualstudio.com/>
  - 推荐插件：Volar、ESLint、Prettier

---

## 三、Linux 环境搭建

以下以 **Ubuntu 20.04/22.04** 和 **CentOS 7/8** 为例。

### 3.1 JDK 17+

**Ubuntu：**

```bash
# 方式一：apt 安装 OpenJDK（推荐）
sudo apt update
sudo apt install -y openjdk-17-jdk

# 验证
java -version
```

**CentOS：**

```bash
# 方式一：yum 安装 OpenJDK
yum install -y java-17-openjdk java-17-openjdk-devel

# 验证
java -version
```

**配置 JAVA_HOME（可选）：**

```bash
# 查看 Java 安装路径
readlink -f $(which java)

# 编辑 /etc/profile 或 ~/.bashrc
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc
source ~/.bashrc
```

### 3.2 Maven（可选）

**Ubuntu / CentOS 通用：**

```bash
# 下载（版本号以官网最新为准）
cd /opt
wget https://dlcdn.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz
tar -zxvf apache-maven-3.9.6-bin.tar.gz

# 配置环境变量
echo 'export MAVEN_HOME=/opt/apache-maven-3.9.6' >> ~/.bashrc
echo 'export PATH=$MAVEN_HOME/bin:$PATH' >> ~/.bashrc
source ~/.bashrc

# 验证
mvn -v
```

> 也可以直接用项目自带的 Maven Wrapper：`./mvnw -v`

### 3.3 Node.js 18+

**Ubuntu：**

```bash
# 方式一：使用 NodeSource 仓库（推荐，版本新）
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt install -y nodejs

# 验证
node -v
npm -v
```

**CentOS：**

```bash
# 方式一：使用 NodeSource 仓库
curl -fsSL https://rpm.nodesource.com/setup_18.x | bash -
yum install -y nodejs

# 验证
node -v
npm -v
```

### 3.4 MySQL 8.0

详见 [mysql-deployment.md](./mysql-deployment.md) 第二章。

### 3.5 Redis（可选）

详见 [redis-deployment.md](./redis-deployment.md) 第三章。

### 3.6 Git

**Ubuntu：**

```bash
sudo apt install -y git
git --version
```

**CentOS：**

```bash
yum install -y git
git --version
```

**配置用户信息：**

```bash
git config --global user.name "你的名字"
git config --global user.email "你的邮箱"
```

---

## 四、环境验证命令汇总

安装完成后，执行以下命令逐一验证：

| 软件 | 验证命令 | 预期输出示例 |
|------|----------|--------------|
| JDK | `java -version` | `openjdk version "17.0.x"` |
| Maven | `mvn -v` | `Apache Maven 3.9.x` |
| Node.js | `node -v` | `v18.x.x` |
| npm | `npm -v` | `9.x.x` |
| MySQL | `mysql -u root -p123456 -e "SELECT VERSION();"` | `8.0.x` |
| Redis | `redis-cli ping` | `PONG` |
| Git | `git --version` | `git version 2.x.x` |

> 所有命令均返回正常版本信息即环境准备完成。

---

## 五、国内镜像加速配置

### 5.1 npm 淘宝源

国内网络环境下，npm 默认源下载较慢，建议切换为淘宝镜像。

```bash
# 设置淘宝源
npm config set registry https://registry.npmmirror.com

# 验证
npm config get registry
# 应输出：https://registry.npmmirror.com

# 如需恢复官方源
# npm config set registry https://registry.npmjs.org/
```

**使用 pnpm（可选，更快）：**

```bash
# 安装 pnpm
npm install -g pnpm

# 设置淘宝源
pnpm config set registry https://registry.npmmirror.com
```

### 5.2 Maven 阿里云镜像

编辑 Maven 的 `settings.xml` 文件（路径：`~/.m2/settings.xml` 或 Maven 安装目录 `conf/settings.xml`）：

```xml
<settings>
  <mirrors>
    <mirror>
      <id>aliyunmaven</id>
      <mirrorOf>*</mirrorOf>
      <name>阿里云公共仓库</name>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
</settings>
```

> IDEA 用户也可以在 Settings → Build, Execution, Deployment → Build Tools → Maven 中配置。

### 5.3 其他加速建议

- **Docker 镜像**：配置国内镜像加速器（阿里云、网易云等）
- **GitHub 访问慢**：使用 Git 代理或镜像站（如 `ghproxy.com`）
- **VS Code 插件**：设置 `extensions.autoUpdate` 为 false，手动更新

---

## 环境检查清单

- [ ] JDK 17+ 已安装，`java -version` 正常
- [ ] Maven 已安装（或使用 IDE 内置 / Wrapper）
- [ ] Node.js 18+ 已安装，`node -v` 正常
- [ ] npm 可用，已配置淘宝镜像源
- [ ] MySQL 8.0 已安装并启动
- [ ] Redis 已安装并启动（可选）
- [ ] Git 已安装，用户信息已配置
- [ ] IDE 已安装（IDEA / VS Code）
