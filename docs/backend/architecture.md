> 【文档重构】从 01-技术选型明细.md / 02-项目目录结构.md 提取重组，仅结构调整

# 后端架构说明

本文档从技术选型与项目目录结构中提取后端架构相关内容，便于后端开发快速查阅。

## 一、技术栈

| 层次 | 选型 | 版本 |
| --- | --- | --- |
| 后端框架 | Spring Boot（单体模块化） | 3.2.x / JDK 17 |
| ORM | MyBatis-Plus | 3.5.x |
| 数据库 | MySQL | 8.0 |
| 缓存 | Redis | 7.x |
| 鉴权 | JWT（jjwt） | 0.12.x |
| 接口文档 | springdoc-openapi（Swagger UI） | 2.4.x |

**选择理由**：
- 与银行现有系统（Java 系）技术栈同族；生态成熟、资料多；模块化包结构预留微服务拆分
- MyBatis-Plus CRUD 开箱即用、国内主流，降低骨架开发量，复杂查询与银行 SQL 风格贴合
- MySQL 关系型、事务成熟，25 张表直接落地
- JWT 无状态、前后端分离友好
- springdoc-openapi 演示与答辩时可直接查看接口

## 二、架构风格：单体模块化

一期骨架采用**单体 + 模块分包**（`module/user`、`module/guarantee`…）。

**理由**：
- 演示系统规模小，单体部署与调试成本最低
- 包结构已按模块划分，未来按 `module` 边界拆分微服务不需重构业务代码

## 三、分层架构

后端代码按以下层次组织：

```
backend/src/main/java/com/icbc/qingqi/
├── QingqiApplication.java    # 启动类
├── common/                   # 通用层：统一返回、错误码、异常处理
│   ├── Result.java
│   ├── ErrorCode.java
│   ├── BizException.java
│   └── GlobalExceptionHandler.java
├── config/                   # 配置层：跨域、MyBatis-Plus
│   ├── CorsConfig.java
│   └── MybatisPlusConfig.java
├── security/                 # 安全层：JWT 签发/校验、登录上下文
│   ├── JwtUtil.java
│   ├── JwtAuthFilter.java
│   └── UserContext.java
└── module/                   # 业务模块层（对应五大模块＋公共支撑）
    ├── user/                 # 公共支撑：用户注册/登录/授权
    ├── guarantee/            # 模块1 安居金融风控（租房履约保函）
    ├── loan/                 # 模块2 轻创业智能授信（青创e贷 A/B）
    ├── budget/               # 模块4 碎片消费治理（分类预算/提醒/转储蓄）
    ├── bookkeeping/          # 模块3 创业经营赋能（占位，二期完善）
    ├── safety/               # 模块5 青年金融安全（反诈/骗局甄别）
    └── message/              # 公共支撑：消息提醒中心
```

**分层规则**：
- 每个 `module/*` 内保持 `Entity + Mapper + Service + Controller` 四件套，新增模块按此复制扩展
- `common`、`security`、`config` 为横向能力，不允许业务模块互相直接引用对方内部类
- 二期新增"数据中台/画像"时建议在 `module` 同级增加 `engine/` 目录承载画像与规则引擎

## 四、与设计说明书分层的对应关系

| 设计说明书分层 | 后端落点 |
| --- | --- |
| 网关层 | JWT 过滤器（JwtAuthFilter） |
| 应用服务层 | `module/*` 下的 Controller/Service |
| 智能引擎层 | 骨架阶段以服务内规则模拟（AI 合同复审、预审规则、骗局甄别规则） |
| 业务中台层 | 模拟桩：银行能力由服务层直接模拟，预留适配点 |
| 数据层 | MySQL（schema.sql 25 张表）+ Redis（预留） |

## 五、关键设计取舍

1. **银行能力用模拟桩**：放款、保函开立、征信查询等银行侧能力不真实接入，由服务层模拟规则返回（如 B 类预审"不查征信、给额度区间"）。模拟桩保证演示闭环完整，且预留了替换为真实接口的适配点。
2. **密码散列先简化**：骨架阶段用 SHA-256 散列存储演示账号密码；正式化须替换为加盐算法（如 BCrypt）并通过安全评审。已在代码注释中标注。
3. **Redis 一期不作为强依赖**：docker-compose 中预留 Redis 容器，但后端未硬依赖，避免骨架阶段因 Redis 未启动而无法运行。

## 六、接口契约

- 接口前缀：`/api/v1`
- 统一返回：`{"code": 0, "message": "success", "data": {...}}`
- 错误码：0/1001/1002/2001/2002/3001/3002/4001/5000（与设计说明书 8.2 一致）
- 鉴权方式：`Authorization: Bearer <JWT>`
- 免登录路径：`/api/v1/auth/**`
