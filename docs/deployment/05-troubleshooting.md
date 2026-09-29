# 青启e城 · 常见问题排查总览

> 本文档属于「本地部署与配置指南」系列，完整目录见 [README.md](./README.md)

> 适用场景：项目部署和运行过程中遇到各类问题时的排查与解决参考。
> 按类别分组，每个问题包含：现象、原因、解决方法。如未找到答案，可参考文末的排查思路自行定位。

---

## 目录

- [排查思路总流程图](#排查思路总流程图)
- [如何查看日志](#如何查看日志)
- [一、环境类问题](#一环境类问题)
- [二、数据库类问题](#二数据库类问题)
- [三、Redis 类问题](#三redis-类问题)
- [四、后端启动类问题](#四后端启动类问题)
- [五、前端启动类问题](#五前端启动类问题)
- [六、功能类问题](#六功能类问题)

---

## 排查思路总流程图

```
发现问题
   ↓
查看报错信息（控制台/日志/浏览器）
   ↓
判断所属类别：环境 / 数据库 / Redis / 后端 / 前端 / 功能
   ↓
在本文档对应分类中搜索相似问题
   ↓
按照解决方法尝试修复
   ↓
验证是否解决 → 是 → 结束
   ↓ 否
查看详细日志，进一步定位
   ↓
搜索技术社区 / 官方文档
   ↓
提交 Issue / 寻求帮助
```

**排查原则：**
1. **先看日志** — 大多数问题的答案都在日志里
2. **由近及远** — 先看最直接的报错，再向上追溯
3. **排除法** — 逐个排除可能的原因，缩小范围
4. **搜索引擎** — 把错误信息复制到搜索引擎，通常能找到解决方案

---

## 如何查看日志

### 后端日志

**开发环境（Maven / IDEA 启动）：**
- 日志直接输出在控制台
- IDEA：底部 `Run` 或 `Debug` 面板
- 关键信息：启动时的 `Started QingqiApplication`、异常堆栈（Exception stack trace）

**生产环境（JAR 包部署）：**
- 项目默认**不写日志文件**，日志直接输出到控制台（application.yml 未配置 `logging.file.name`）
- 如需文件日志，在 `application.yml` 中添加 `logging.file.name: logs/qingqi.log`，重启后生效
- 实时查看：`tail -f logs/qingqi.log`（Linux）或 `Get-Content logs/qingqi.log -Wait`（Windows）

### 前端日志

**浏览器控制台：**
- 打开方式：F12 或右键 → 检查 → Console 标签
- 查看 JS 报错、网络请求
- Network 标签可查看接口请求状态、响应数据

**终端日志：**
- Vite 开发服务器的日志在启动终端中
- 构建错误、代理错误会显示在这里

### 数据库日志

**MySQL：**
- Windows：MySQL 安装目录下 `data/` 中 `.err` 文件
- Linux：`/var/log/mysqld.log`（CentOS）或 `/var/log/mysql/error.log`（Ubuntu）

**Redis：**
- 默认输出在启动终端
- 配置了 `logfile` 则在对应文件中

---

## 一、环境类问题

### 1.1 Java 版本不对

**现象：** 启动后端时报错，提示 `UnsupportedClassVersionError` 或 `java.lang.UnsupportedClassVersionError`。

**原因：** 编译时使用的 JDK 版本高于运行时的 JDK 版本。项目要求 JDK 17+，但本地安装的是 JDK 8 或 11。

**解决方法：**

```bash
# 查看当前 Java 版本
java -version

# 查看 javac 版本
javac -version
```

1. 确认已安装 JDK 17+（下载见 [01-env-preparation.md](./01-env-preparation.md)）
2. 配置 `JAVA_HOME` 指向 JDK 17 安装目录
3. 确保 `Path` 中 `%JAVA_HOME%\bin` 在其他 Java 路径之前
4. 重启终端或 IDE 后重新验证

> IDEA 用户：File → Project Structure → Project → SDK 选择 17

### 1.2 Maven 下载依赖慢

**现象：** `mvn clean install` 或 IDEA 导入项目时，下载依赖非常慢，甚至超时失败。

**原因：** Maven 默认从中央仓库下载，国内网络访问慢。

**解决方法：**

配置阿里云镜像，见 [01-env-preparation.md](./01-env-preparation.md#52-maven-阿里云镜像)。

```xml
<!-- 在 ~/.m2/settings.xml 中添加 -->
<mirror>
  <id>aliyunmaven</id>
  <mirrorOf>*</mirrorOf>
  <name>阿里云公共仓库</name>
  <url>https://maven.aliyun.com/repository/public</url>
</mirror>
```

### 1.3 Node.js 版本不兼容

**现象：** 执行 `npm install` 或 `npm run dev` 时报错，提示 `ERR! Unsupported engine` 或语法错误。

**原因：** 本地 Node.js 版本过低（项目要求 18+）。

**解决方法：**

```bash
# 查看当前版本
node -v

# 如果低于 18.x，需要升级
```

升级方式：
- Windows：重新下载安装包覆盖安装
- Linux / Mac：使用 nvm（Node Version Manager）管理多版本

```bash
# 使用 nvm 安装 18.x
nvm install 18
nvm use 18
nvm alias default 18
```

### 1.4 npm install 失败

**现象：** 执行 `npm install` 时报错，常见错误包括网络超时、权限问题、依赖冲突等。

**常见原因及解决：**

**原因 1：网络超时**

```bash
# 配置淘宝镜像源
npm config set registry https://registry.npmmirror.com

# 清理缓存后重试
npm cache clean --force
rm -rf node_modules package-lock.json
npm install
```

**原因 2：权限不足（Linux/Mac）**

```bash
# 使用 sudo
sudo npm install

# 或修复 npm 全局目录权限
sudo chown -R $USER ~/.npm
sudo chown -R $USER /usr/local/lib/node_modules
```

**原因 3：依赖版本冲突**

```bash
# 查看冲突信息
npm ls <包名>

# 删除 node_modules 和 lock 文件，重新安装
rm -rf node_modules package-lock.json
npm install

# 如仍有冲突，尝试使用 --legacy-peer-deps
npm install --legacy-peer-deps
```

**原因 4：Windows 下 node-gyp 编译失败**

需要安装 Windows 构建工具：

```cmd
# 以管理员身份运行 cmd
npm install -g windows-build-tools
```

或使用 VS Installer 安装 "Desktop development with C++" 工作负载。

---

## 二、数据库类问题

### 2.1 数据库连接失败

**现象：** 后端启动时报错 `Communications link failure` 或 `Connection refused`。

**原因排查：**

1. **MySQL 服务未启动**
   ```cmd
   # Windows 检查服务
   sc query mysql
   # 启动服务
   net start mysql

   # Linux 检查服务
   systemctl status mysqld
   # 启动服务
   systemctl start mysqld
   ```

2. **端口或地址不对**
   ```bash
   # 检查端口监听
   netstat -ano | findstr 3306   # Windows
   netstat -tlnp | grep 3306      # Linux
   ```
   确认 application.yml 中的端口和实际端口一致。

3. **防火墙拦截**
   - 本地一般无此问题，远程连接需检查防火墙
   - 参考 [mysql-deployment.md](./mysql-deployment.md#四开启远程访问内网测试用)

4. **bind-address 配置**
   - 远程连接时，MySQL 需要绑定 `0.0.0.0` 而不是 `127.0.0.1`

### 2.2 表不存在（Table 'xxx' doesn't exist）

**现象：** 后端启动或调用接口时报错 `Table 'qingqi.xxx' doesn't exist`。

**原因：**
- 数据库中缺少对应的表
- schema.sql 未导入或导入失败
- 数据库名配置错误（连错了数据库）

**解决方法：**

```sql
-- 1. 确认当前数据库
SELECT DATABASE();

-- 2. 查看表列表
USE qingqi;
SHOW TABLES;

-- 3. 检查表数量（应为 43 张）
SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'qingqi';
```

如果表数量不对，重新导入 schema.sql：

```cmd
mysql -u root -p123456 qingqi < schema.sql
```

### 2.3 中文乱码

**现象：** 页面或接口返回的中文显示为问号（?）或乱码。

**原因：**
- 数据库字符集不是 utf8mb4
- JDBC URL 缺少字符集参数
- 前端编码问题

**解决方法：**

**步骤 1：检查数据库字符集**

```sql
-- 查看数据库字符集
SHOW CREATE DATABASE qingqi;

-- 查看表字符集
SHOW CREATE TABLE sys_user;
```

如果不是 utf8mb4，修改：

```sql
-- 修改数据库字符集
ALTER DATABASE qingqi CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- 修改表字符集（对每张表执行）
ALTER TABLE sys_user CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

**步骤 2：检查 JDBC URL**

确保 URL 中包含 `useUnicode=true&characterEncoding=utf8`：

```
jdbc:mysql://127.0.0.1:3306/qingqi?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
```

### 2.4 密码错误（Access denied）

**现象：** 报错 `Access denied for user 'root'@'localhost' (using password: YES)`。

**原因：**
- 密码输入错误
- 用户不存在
- 用户 host 不匹配

**解决方法：**

1. 确认密码是否正确（默认测试密码 `123456`）
2. 检查 application.yml 中的密码配置
3. 如忘记密码，参考 [mysql-deployment.md](./mysql-deployment.md#常见报错排查) 重置密码

### 2.5 远程连接不上

**现象：** 远程机器连接 MySQL 时报错 `Can't connect to MySQL server` 或 `Host 'xxx' is not allowed`。

**原因及解决：**

| 错误信息 | 原因 | 解决方法 |
|----------|------|----------|
| `Can't connect to MySQL server` | 网络不通 / 防火墙拦截 / bind-address 未配置 | 检查网络、防火墙、bind-address |
| `Host 'xxx' is not allowed` | 没有远程访问账号 | 创建 `root@'%'` 账号 |
| `Access denied` | 远程账号密码错误 | 重新设置远程账号密码 |

详细步骤见 [mysql-deployment.md](./mysql-deployment.md#四开启远程访问内网测试用)。

---

## 三、Redis 类问题

### 3.1 Redis 连接失败

**现象：** 后端启动或运行时报错 `RedisConnectionFailureException` 或 `Unable to connect to Redis`。

**原因排查：**

1. **Redis 服务未启动**
   ```cmd
   # Windows
   redis-cli ping
   # 如报错，启动 Redis：redis-server.exe redis.windows.conf

   # Linux
   systemctl status redis
   systemctl start redis
   ```

2. **地址或端口不对**
   - 检查 application.yml 中的 host 和 port
   - 默认端口 6379

3. **密码错误**
   - 如果 Redis 设置了密码，确保配置文件中 `password` 正确
   - 无密码时，`password` 留空或删除该行

### 3.2 认证错误（NOAUTH Authentication required）

**现象：** 操作 Redis 时报错 `(error) NOAUTH Authentication required`。

**原因：** Redis 设置了密码，但连接时没有提供密码。

**解决方法：**

```yaml
# application.yml 中添加密码
spring:
  data:
    redis:
      password: "123456"   # 你的 Redis 密码
```

或命令行测试时：

```bash
redis-cli -a 123456 ping
# 或登录后认证
redis-cli
127.0.0.1:6379> AUTH 123456
```

### 3.3 保护模式错误

**现象：** 报错 `DENIED Redis is running in protected mode`。

**原因：** Redis 开启了保护模式，远程访问且没有设置密码。

**解决方法（三选一）：**

1. **设置密码**（推荐）
   ```
   requirepass 123456
   ```

2. **关闭保护模式**（仅限内网测试）
   ```
   protected-mode no
   ```

3. **绑定特定 IP**（不监听 0.0.0.0）
   ```
   bind 127.0.0.1 192.168.1.100
   ```

更多 Redis 问题见 [redis-deployment.md](./redis-deployment.md#五常见报错排查)。

---

## 四、后端启动类问题

### 4.1 端口被占用

**现象：** 启动时报错 `Web server failed to start. Port 8080 was already in use.`

**原因：** 8080 端口已被其他程序占用。

**解决方法：**

**方式 1：查找并结束占用进程**

```cmd
# Windows：查找占用 8080 端口的进程
netstat -ano | findstr 8080
# 找到 PID（最后一列数字），结束进程
taskkill /F /PID <进程PID>
```

```bash
# Linux / Mac
lsof -i :8080
kill -9 <进程PID>
```

**方式 2：修改后端端口**

编辑 `application.yml`：

```yaml
server:
  port: 8081   # 改为可用端口
```

> 修改端口后，前端 Vite 代理配置中的 target 端口也要同步修改。

### 4.2 依赖注入失败（BeanCreationException）

**现象：** 启动时报错 `BeanCreationException` 或 `NoSuchBeanDefinitionException`。

**原因：**
- Spring 容器无法创建某个 Bean
- 依赖的 Bean 不存在
- 循环依赖
- Mapper 扫描路径配置错误

**排查步骤：**

1. 查看完整异常堆栈，找到 Caused by 后的具体错误
2. 检查报错的类是否有 `@Service`、`@Component`、`@Mapper` 等注解
3. 检查 MyBatis-Plus 的 `mapper-locations` 和 `type-aliases-package` 配置
4. 检查启动类 `@SpringBootApplication` 扫描范围是否覆盖对应包

**常见情况：**
- Mapper 接口缺少 `@Mapper` 注解 → 添加注解或在启动类加 `@MapperScan`
- 包名不匹配 → 确保 Mapper 在 `com.icbc.qingqi.module.**.mapper` 路径下

### 4.3 数据库连接池报错

**现象：** 报错 `HikariPool-1 - Exception during pool initialization`。

**原因：** HikariCP 连接池无法初始化数据库连接。

**排查步骤：**

1. 查看 Caused by 后的具体错误（通常是连接失败 / 密码错误 / 数据库不存在）
2. 确认 MySQL 服务已启动
3. 确认 application.yml 中数据库配置正确
4. 确认数据库 `qingqi` 已创建

常见子错误：
- `Unknown database 'qingqi'` → 先创建数据库
- `Access denied` → 用户名或密码错误
- `Communications link failure` → 数据库连接不通

### 4.4 ClassNotFoundException / NoClassDefFoundError

**现象：** 启动时报某个类找不到。

**原因：**
- 缺少依赖
- 依赖版本冲突
- Maven 依赖下载不完整

**解决方法：**

```bash
# 1. 清理并重新编译
mvn clean compile

# 2. 强制更新依赖
mvn clean install -U

# 3. 检查依赖树
mvn dependency:tree | findstr <缺失的包名>
```

IDEA 用户：
- 右键 pom.xml → Maven → Reload Project
- 或点击右侧 Maven 面板的刷新按钮

### 4.5 启动后立刻退出

**现象：** 启动后没有报错，但进程立刻退出，没有 `Started QingqiApplication` 字样。

**原因：**
- 启动类写错，不是 Spring Boot 应用
- 缺少 Web 依赖（spring-boot-starter-web）
- 应用以非 Web 方式运行

**排查：**
1. 确认启动类有 `@SpringBootApplication` 注解
2. 确认 pom.xml 中包含 `spring-boot-starter-web` 依赖
3. 查看启动日志中是否有 Spring Boot banner 输出

---

## 五、前端启动类问题

### 5.1 端口被占用

**现象：** 启动时提示 `Port 5173 is already in use`。

**原因：** 端口被其他程序占用（或上一次启动的进程没关干净）。

**解决方法：**

**方式 1：修改前端端口**

修改 `vite.config.js` / `vite.config.ts` 中的 `server.port`（本项目未使用 .env 文件）：

```env
VITE_PORT=5175
```

**方式 2：结束占用进程**

```cmd
# Windows
netstat -ano | findstr 5173
taskkill /F /PID <PID>
```

```bash
# Linux / Mac
lsof -i :5173
kill -9 <PID>
```

### 5.2 依赖缺失（Cannot find module）

**现象：** 启动时报错 `Error: Cannot find module 'xxx'`。

**原因：**
- `npm install` 未执行或执行失败
- 缺少某个依赖包
- node_modules 损坏

**解决方法：**

```bash
# 删除 node_modules 和 lock 文件，重新安装
rm -rf node_modules package-lock.json
npm install
```

Windows PowerShell：

```powershell
Remove-Item -Recurse -Force node_modules, package-lock.json
npm install
```

### 5.3 跨域问题（CORS error）

**现象：** 浏览器控制台报错 `Access to XMLHttpRequest at '...' from origin '...' has been blocked by CORS policy`。

**原因：** 前端请求后端接口时跨域了。

**解决方法：**

**开发环境（推荐）：** 使用 Vite 代理

确保 `vite.config.js` 中配置了代理：

```javascript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

同时确保前端请求使用 `/api` 前缀，而不是完整 URL。

**后端方案：** 配置跨域过滤器

项目中已有 `CorsConfig.java`，确认其生效即可。

> 注意：如果前端直接请求完整的后端地址（如 `http://localhost:8080/api/xxx`），代理不会生效，需要依赖后端 CORS 配置。

### 5.4 页面白屏

**现象：** 访问前端页面一片空白，控制台有报错。

**常见原因及解决：**

| 原因 | 排查方法 | 解决方法 |
|------|----------|----------|
| JS 加载失败 | 控制台看报错 | 检查 `npm run dev` 输出，修复编译错误 |
| 路由配置错误 | 控制台看路由报错 | 检查 router/index.js 路由配置 |
| 组件导入路径错误 | 控制台看 404 错误 | 检查 import 路径是否正确 |
| Pinia 未注册 | 控制台看 Pinia 相关报错 | 确认 main.js 中注册了 Pinia |
| 资源路径错误 | Network 标签看 404 | 检查静态资源引用路径 |

**快速定位：** 打开浏览器控制台（F12），查看 Console 和 Network 中的错误信息。

### 5.5 热更新不生效

**现象：** 修改代码后浏览器没有自动刷新。

**原因：**
- Vite HMR 配置问题
- 文件路径大小写问题（Windows 不区分大小写）
- 某些特殊语法导致 HMR 失效

**解决方法：**

1. 检查文件名大小写是否与 import 一致
2. 尝试手动刷新浏览器
3. 重启 Vite 开发服务器
4. 检查 vite.config.js 中是否有影响 HMR 的配置

---

## 六、功能类问题

### 6.1 登录失败

**现象：** 输入账号密码点击登录，提示登录失败或无响应。

**排查步骤：**

1. **前端控制台检查**
   - F12 打开控制台，看是否有 JS 报错
   - Network 标签看登录接口请求状态

2. **接口返回分析**
   - 404：接口路径不对，检查前端 API 路径和后端接口路径
   - 401：账号或密码错误
   - 500：后端服务报错，查看后端日志
   - CORS 错误：跨域问题，见 5.3 节

3. **数据库检查**
   ```sql
   -- 检查用户是否存在
   SELECT * FROM sys_user WHERE username = 'admin';

   -- 检查密码（存储的是加密后的）
   SELECT username, password FROM sys_user WHERE username = 'admin';
   ```

4. **常见原因**
   - 密码错误（演示密码为 `123456`）
   - 用户不存在（data.sql 未导入）
    - 密码加密方式不匹配（项目统一使用 Spring Security `BCryptPasswordEncoder`，与 data.sql 中演示账号的 BCrypt 哈希对应；若自行插入用户，需用 BCrypt 加密）

### 6.2 接口 404

**现象：** 调用接口返回 404 Not Found。

**排查步骤：**

1. **确认后端服务是否启动**
   - 访问 `http://localhost:8080/api/doc.html` 看能否打开
   - 访问 `http://localhost:8080/api/health` 看是否返回正常

2. **检查接口路径**
   - 前端请求路径是否正确（是否有 `/api` 前缀）
   - 后端 Controller 的 `@RequestMapping` 路径是否匹配
   - 注意大小写和拼写

3. **检查代理配置**
   - 开发环境是否通过 Vite 代理转发
   - `vite.config.js` 中 `target` 是否正确

4. **检查 Context-Path**
   - 如果后端配置了 `server.servlet.context-path`，前端请求需要加上对应前缀

### 6.3 权限不足（403 Forbidden）

**现象：** 访问某个接口或页面时提示 403 无权限。

**原因：**
- 登录用户角色没有对应权限
- Token 无效或过期
- 接口需要特定角色才能访问

**解决方法：**

1. 确认登录账号的角色
   ```sql
   SELECT u.username, r.role_code, r.role_name
   FROM sys_user u
    LEFT JOIN sys_user_role ur ON u.id = ur.user_id
    LEFT JOIN sys_role r ON ur.role_id = r.id
   WHERE u.username = 'admin';
   ```

2. 检查接口的权限注解（`@PreAuthorize` 或自定义权限校验）
3. 管理端菜单配置的 roles 是否包含当前用户角色
4. 确认 Token 是否有效，重新登录获取新 Token

### 6.4 数据不显示

**现象：** 页面打开后没有数据，表格为空。

**排查步骤：**

1. **检查接口返回**
   - F12 → Network → 找到对应接口
   - 看 Response 中是否有数据
    - 看返回的 code 是否为 0（本项目约定 code=0 成功）

2. **检查数据库数据**
   ```sql
   -- 查看对应表是否有数据
   SELECT COUNT(*) FROM 表名;
   SELECT * FROM 表名 LIMIT 10;
   ```

3. **常见原因**
   - 数据库中没有数据 → 导入 data.sql
   - SQL 查询条件过滤掉了 → 检查 SQL 和参数
   - 前端数据绑定错误 → 检查页面代码
   - 接口返回数据结构和前端期望不一致 → 统一数据格式

### 6.5 上传文件失败

**现象：** 上传文件时提示失败或无响应。

**常见原因：**

1. **文件大小超过限制**
   - 后端 `spring.servlet.multipart.max-file-size` 配置（默认 10MB）
   - Nginx 也可能有限制（`client_max_body_size`）

2. **上传目录不存在或无权限**
   - 检查上传路径配置
   - 确保目录存在且有写入权限

3. **文件类型不允许**
   - 检查后端是否有文件类型校验
   - 确认上传的文件类型在白名单内

### 6.6 AI / LLM 调用失败（消息标注"AI 降级本地"）

**现象：** 智能客服或反诈情景演练的消息标注"AI 降级本地"；或后端日志出现 `[AgentLLM] 调用失败`。

**原因与排查（按顺序）：**

1. **LLM 服务未启动（最常见）**
   - 方案 A（本机 Ollama）：`curl http://localhost:11434/` 应返回 `HTTP 200`；无响应则 `ollama serve` 启动，并配置开机自启
   - 方案 B（云端 API）：检查 `.env` 中 `CHAT_LLM_API_KEY` 是否有效、是否欠费

2. **模型未拉取 / 模型名不匹配**
   - `ollama list` 确认模型存在，`.env` 中 `CHAT_LLM_MODEL` 与模型名完全一致（如 `qwen2.5:3b-instruct`）

3. **请求超时（慢）**
   - 模型冷加载：设置 `OLLAMA_KEEP_ALIVE=-1` 常驻内存
   - 输出过长 / 历史过长：确认后端 `chat.llm.max-tokens=256`、历史自动截断最近 3 轮（新版已默认）

4. **配置未生效**
   - 修改 `.env` 后需重建后端：`docker compose up -d --build backend`
   - 确认引擎状态：登录后访问 `/api/v1/chat/engine-status`，期望 `activeEngine=agent`、`agentAvailable=true`

> 完整配置与验证见 [06-ai-llm.md](./06-ai-llm.md)。

---

## 提交 Issue 前的信息收集

如果以上方法都无法解决问题，在寻求帮助时请提供以下信息：

1. **操作系统**：Windows / Linux（发行版）/ macOS
2. **软件版本**：JDK 版本、Node 版本、MySQL 版本
3. **错误信息**：完整的报错信息和堆栈（截图或文本）
4. **复现步骤**：做了什么操作后出现的问题
5. **已尝试的解决方法**：已经试过哪些方案，结果如何
6. **相关配置**：关键配置文件内容（注意隐藏敏感信息）

> 提供的信息越详细，问题解决越快。

---

## 相关文档索引

| 问题类型 | 参考文档 |
|----------|----------|
| MySQL 部署与问题 | [mysql-deployment.md](./mysql-deployment.md) |
| Redis 部署与问题 | [redis-deployment.md](./redis-deployment.md) |
| 后端配置 | [03-backend-config.md](./03-backend-config.md) |
| 前端配置 | [04-frontend-config.md](./04-frontend-config.md) |
| Docker 部署 | [docker-compose.md](./docker-compose.md) |
