-- =============================================================
-- 青启e城 数据库设计 schema.sql
-- 表数量：25 张（对齐设计说明书第 6 章）
-- 表命名规则：sys_* 系统公共表 / biz_* 业务模块表
-- 金额字段统一使用 DECIMAL(18,2)，禁止使用 FLOAT/DOUBLE
-- 字符集：utf8mb4 / 引擎：InnoDB
-- =============================================================
CREATE DATABASE IF NOT EXISTS qingqi DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE qingqi;

-- =============================================================
-- 一、公共支撑 - 用户与权限（4张）
-- =============================================================

-- -------------------------------------------------------------
-- 1. sys_user 用户表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username`        VARCHAR(50)  NOT NULL COMMENT '用户名/登录账号',
  `password`        VARCHAR(100) NOT NULL COMMENT '密码（BCrypt加密）',
  `nickname`        VARCHAR(50)  NULL COMMENT '昵称',
  `real_name`       VARCHAR(50)  NULL COMMENT '真实姓名',
  `id_card`         VARCHAR(20)  NULL COMMENT '身份证号（脱敏存储）',
  `phone`           VARCHAR(20)  NULL COMMENT '手机号',
  `email`           VARCHAR(100) NULL COMMENT '邮箱',
  `avatar`          VARCHAR(500) NULL COMMENT '头像URL',
  `role`            VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '角色（冗余字段，快速查询）',
  `user_type`       VARCHAR(20)  NULL COMMENT '人群类型：STUDENT/GRADUATE/ENTREPRENEUR/OTHER',
  `graduation_date` DATE         NULL COMMENT '毕业日期',
  `school`          VARCHAR(100) NULL COMMENT '学校',
  `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1正常',
  `deleted`         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0存在 1删除',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_phone` (`phone`),
  KEY `idx_user_type` (`user_type`),
  KEY `idx_role_status` (`role`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- -------------------------------------------------------------
-- 2. sys_role 角色表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_code`   VARCHAR(50)  NOT NULL COMMENT '角色编码',
  `role_name`   VARCHAR(50)  NOT NULL COMMENT '角色名称',
  `description` VARCHAR(200) NULL COMMENT '描述',
  `sort_order`  INT          NOT NULL DEFAULT 0 COMMENT '排序',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1正常',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- -------------------------------------------------------------
-- 3. sys_user_role 用户角色关联表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
  `role_id`     BIGINT   NOT NULL COMMENT '角色ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- -------------------------------------------------------------
-- 4. sys_login_log 登录日志表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `sys_login_log`;
CREATE TABLE `sys_login_log` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`     BIGINT       NULL COMMENT '用户ID',
  `username`    VARCHAR(50)  NULL COMMENT '登录账号',
  `login_ip`    VARCHAR(50)  NULL COMMENT '登录IP',
  `login_type`  VARCHAR(20)  NULL COMMENT '登录方式：PASSWORD/WECHAT',
  `status`      TINYINT      NOT NULL COMMENT '状态：0失败 1成功',
  `fail_reason` VARCHAR(200) NULL COMMENT '失败原因',
  `user_agent`  VARCHAR(500) NULL COMMENT '浏览器UA',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_username` (`username`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';

-- =============================================================
-- 二、公共支撑 - 消息中心（1张）
-- =============================================================

-- -------------------------------------------------------------
-- 5. sys_message 消息表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `sys_message`;
CREATE TABLE `sys_message` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`     BIGINT        NOT NULL COMMENT '接收用户ID',
  `title`       VARCHAR(100)  NOT NULL COMMENT '消息标题',
  `content`     VARCHAR(1000) NOT NULL COMMENT '消息内容',
  `type`        VARCHAR(20)   NOT NULL DEFAULT 'SYSTEM' COMMENT '消息类型：SYSTEM/BUDGET/BUSINESS/SAFETY',
  `biz_type`    VARCHAR(30)   NULL COMMENT '业务类型：GUARANTEE/LOAN 等',
  `biz_id`      BIGINT        NULL COMMENT '关联业务ID',
  `is_read`     TINYINT       NOT NULL DEFAULT 0 COMMENT '是否已读：0未读 1已读',
  `read_time`   DATETIME      NULL COMMENT '阅读时间',
  `deleted`     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0存在 1删除',
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_user_read` (`user_id`, `is_read`),
  KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

-- =============================================================
-- 三、安居金融风控（6张）
-- =============================================================

-- -------------------------------------------------------------
-- 6. biz_landlord 房东信息表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `biz_landlord`;
CREATE TABLE `biz_landlord` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`       BIGINT       NOT NULL COMMENT '对应用户表ID（房东也是系统用户）',
  `real_name`     VARCHAR(50)  NOT NULL COMMENT '真实姓名',
  `id_card`       VARCHAR(20)  NULL COMMENT '身份证号',
  `phone`         VARCHAR(20)  NOT NULL COMMENT '联系电话',
  `bank_account`  VARCHAR(50)  NULL COMMENT '收款银行账号',
  `bank_name`     VARCHAR(100) NULL COMMENT '开户银行',
  `verify_status` VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '认证状态：PENDING/VERIFIED/REJECTED',
  `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0存在 1删除',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_phone` (`phone`),
  KEY `idx_verify_status` (`verify_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房东信息表';

-- -------------------------------------------------------------
-- 7. biz_house 房屋信息表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `biz_house`;
CREATE TABLE `biz_house` (
  `id`             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `landlord_id`    BIGINT         NOT NULL COMMENT '房东ID',
  `house_title`    VARCHAR(200)   NOT NULL COMMENT '房屋标题',
  `province`       VARCHAR(50)    NULL COMMENT '省',
  `city`           VARCHAR(50)    NULL COMMENT '市',
  `district`       VARCHAR(50)    NULL COMMENT '区',
  `address`        VARCHAR(500)   NOT NULL COMMENT '详细地址',
  `house_type`     VARCHAR(20)    NULL COMMENT '房屋类型：APARTMENT/HOUSE 等',
  `area`           DECIMAL(10,2)  NULL COMMENT '面积（平方米）',
  `room_count`     INT            NULL COMMENT '居室数',
  `monthly_rent`   DECIMAL(18,2)  NOT NULL COMMENT '月租金',
  `deposit_amount` DECIMAL(18,2)  NULL COMMENT '押金金额',
  `house_images`   TEXT           NULL COMMENT '房屋图片（JSON数组）',
  `status`         TINYINT        NOT NULL DEFAULT 1 COMMENT '状态：0下架 1出租中',
  `deleted`        TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0存在 1删除',
  `create_time`    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_landlord_id` (`landlord_id`),
  KEY `idx_city` (`city`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房屋信息表';

-- -------------------------------------------------------------
-- 8. biz_rental_contract 租赁合同表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `biz_rental_contract`;
CREATE TABLE `biz_rental_contract` (
  `id`               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `contract_no`      VARCHAR(50)    NOT NULL COMMENT '合同编号',
  `house_id`         BIGINT         NOT NULL COMMENT '房屋ID',
  `landlord_id`      BIGINT         NOT NULL COMMENT '房东ID',
  `tenant_id`        BIGINT         NOT NULL COMMENT '租客用户ID',
  `monthly_rent`     DECIMAL(18,2)  NOT NULL COMMENT '月租金',
  `deposit_amount`   DECIMAL(18,2)  NOT NULL COMMENT '押金金额',
  `rent_start_date`  DATE           NOT NULL COMMENT '租期开始日',
  `rent_end_date`    DATE           NOT NULL COMMENT '租期结束日',
  `pay_method`       VARCHAR(20)    NULL COMMENT '付款方式：MONTHLY/QUARTERLY',
  `contract_file`    VARCHAR(500)   NULL COMMENT '合同文件URL',
  `contract_status`  VARCHAR(20)    NOT NULL DEFAULT 'PENDING' COMMENT '合同状态：PENDING/SIGNED/TERMINATED',
  `sign_date`        DATE           NULL COMMENT '签署日期',
  `deleted`          TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0存在 1删除',
  `create_time`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contract_no` (`contract_no`),
  KEY `idx_house_id` (`house_id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_landlord_id` (`landlord_id`),
  KEY `idx_contract_status` (`contract_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租赁合同表';

-- -------------------------------------------------------------
-- 9. biz_guarantee_application 保函申请表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `biz_guarantee_application`;
CREATE TABLE `biz_guarantee_application` (
  `id`                     BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `apply_no`               VARCHAR(50)    NOT NULL COMMENT '申请编号',
  `tenant_id`              BIGINT         NOT NULL COMMENT '申请人（租客）ID',
  `house_id`               BIGINT         NOT NULL COMMENT '房屋ID',
  `contract_id`            BIGINT         NOT NULL COMMENT '租赁合同ID',
  `landlord_id`            BIGINT         NOT NULL COMMENT '房东ID',
  `deposit_amount`         DECIMAL(18,2)  NOT NULL COMMENT '押金金额（保函金额）',
  `guarantee_rate`         DECIMAL(5,4)   NULL COMMENT '保函费率',
  `guarantee_fee`          DECIMAL(18,2)  NULL COMMENT '保函费',
  `guarantee_period_months` INT           NULL COMMENT '保函期限（月）',
  `applicant_name`         VARCHAR(50)    NULL COMMENT '申请人姓名',
  `applicant_phone`        VARCHAR(20)    NULL COMMENT '申请人电话',
  `landlord_name`          VARCHAR(50)    NULL COMMENT '房东姓名',
  `landlord_phone`         VARCHAR(20)    NULL COMMENT '房东电话',
  `apply_status`           VARCHAR(30)    NOT NULL DEFAULT 'SUBMITTED' COMMENT '申请状态：SUBMITTED/LANDLORD_CONFIRM/AI_REVIEW/MANUAL_REVIEW/PENDING_PAY/APPROVED/REJECTED',
  `ai_review_result`       VARCHAR(20)    NULL COMMENT 'AI复审结果：PASS/RISK_WARNING/MANUAL_REVIEW',
  `ai_review_score`        INT            NULL COMMENT 'AI复审评分',
  `ai_review_detail`       TEXT           NULL COMMENT 'AI复审详情（JSON）',
  `reject_reason`          VARCHAR(500)   NULL COMMENT '拒绝原因',
  `submit_time`            DATETIME       NULL COMMENT '提交时间',
  `landlord_confirm_time`  DATETIME       NULL COMMENT '房东确认时间',
  `sign_content`           VARCHAR(2000)  NULL COMMENT '电子签名内容（Canvas base64 或 CLICK_CONFIRM）',
  `sign_time`              DATETIME       NULL COMMENT '电子签署时间',
  `review_time`            DATETIME       NULL COMMENT '审核时间',
  `deleted`                TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0存在 1删除',
  `create_time`            DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`            DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_apply_no` (`apply_no`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_apply_status` (`apply_status`),
  KEY `idx_house_id` (`house_id`),
  KEY `idx_contract_id` (`contract_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='保函申请表';

-- -------------------------------------------------------------
-- 10. biz_guarantee 保函主表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `biz_guarantee`;
CREATE TABLE `biz_guarantee` (
  `id`               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `guarantee_no`     VARCHAR(50)    NOT NULL COMMENT '保函编号',
  `application_id`   BIGINT         NOT NULL COMMENT '关联申请ID',
  `tenant_id`        BIGINT         NOT NULL COMMENT '租客ID',
  `landlord_id`      BIGINT         NOT NULL COMMENT '房东ID',
  `house_id`         BIGINT         NOT NULL COMMENT '房屋ID',
  `guarantee_amount` DECIMAL(18,2)  NOT NULL COMMENT '保函金额',
  `guarantee_fee`    DECIMAL(18,2)  NOT NULL COMMENT '保函费',
  `effective_date`   DATE           NOT NULL COMMENT '生效日期',
  `expire_date`      DATE           NOT NULL COMMENT '到期日期',
  `guarantee_status` VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE' COMMENT '保函状态：ACTIVE/EXPIRED/CLAIMED/TERMINATED',
  `pay_status`       VARCHAR(20)    NOT NULL DEFAULT 'UNPAID' COMMENT '缴费状态：UNPAID/PAID/REFUNDED',
  `pay_time`         DATETIME       NULL COMMENT '缴费时间',
  `issue_time`       DATETIME       NULL COMMENT '开函时间',
  `deleted`          TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0存在 1删除',
  `create_time`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_guarantee_no` (`guarantee_no`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_landlord_id` (`landlord_id`),
  KEY `idx_guarantee_status` (`guarantee_status`),
  KEY `idx_pay_status` (`pay_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='保函主表';

-- -------------------------------------------------------------
-- 11. biz_guarantee_claim 索赔表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `biz_guarantee_claim`;
CREATE TABLE `biz_guarantee_claim` (
  `id`               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `claim_no`         VARCHAR(50)    NOT NULL COMMENT '索赔编号',
  `guarantee_id`     BIGINT         NOT NULL COMMENT '保函ID',
  `guarantee_no`     VARCHAR(50)    NOT NULL COMMENT '保函编号（冗余）',
  `claimant_id`      BIGINT         NOT NULL COMMENT '索赔人ID（房东）',
  `claimant_name`    VARCHAR(50)    NOT NULL COMMENT '索赔人姓名',
  `tenant_id`        BIGINT         NOT NULL COMMENT '被索赔租客ID',
  `claim_amount`     DECIMAL(18,2)  NOT NULL COMMENT '索赔金额',
  `claim_reason`     TEXT           NOT NULL COMMENT '索赔原因',
  `evidence_files`   TEXT           NULL COMMENT '证据材料（JSON数组）',
  `claim_status`     VARCHAR(30)    NOT NULL DEFAULT 'SUBMITTED' COMMENT '索赔状态：SUBMITTED/AI_REVIEW/MANUAL_REVIEW/APPROVED/REJECTED/DEFENSE_PERIOD/CLOSED',
  `ai_review_result` VARCHAR(20)    NULL COMMENT 'AI初审结果',
  `ai_review_detail` TEXT           NULL COMMENT 'AI初审详情',
  `reject_reason`    VARCHAR(500)   NULL COMMENT '拒绝原因',
  `defense_content`  TEXT           NULL COMMENT '申辩内容',
  `payout_amount`    DECIMAL(18,2)  NULL COMMENT '实际赔付金额',
  `submit_time`      DATETIME       NULL COMMENT '提交时间',
  `review_time`      DATETIME       NULL COMMENT '审核时间',
  `close_time`       DATETIME       NULL COMMENT '结案时间',
  `deleted`          TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0存在 1删除',
  `create_time`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_claim_no` (`claim_no`),
  KEY `idx_guarantee_id` (`guarantee_id`),
  KEY `idx_claimant_id` (`claimant_id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_claim_status` (`claim_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='索赔表';

-- =============================================================
-- 四、青创e贷（4张）
-- =============================================================

-- -------------------------------------------------------------
-- 12. biz_merchant 商户信息表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `biz_merchant`;
CREATE TABLE `biz_merchant` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `merchant_name`    VARCHAR(200) NOT NULL COMMENT '商户名称',
  `merchant_type`    VARCHAR(30)  NULL COMMENT '商户类型：MATERIAL/STALL/PROMOTION/OTHER',
  `contact_name`     VARCHAR(50)  NULL COMMENT '联系人',
  `contact_phone`    VARCHAR(20)  NULL COMMENT '联系电话',
  `address`          VARCHAR(500) NULL COMMENT '地址',
  `business_license` VARCHAR(100) NULL COMMENT '营业执照号',
  `bank_account`     VARCHAR(50)  NULL COMMENT '收款账号',
  `bank_name`        VARCHAR(100) NULL COMMENT '开户银行',
  `verify_status`    VARCHAR(20)  NOT NULL DEFAULT 'VERIFIED' COMMENT '认证状态',
  `status`           TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1正常',
  `deleted`          TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0存在 1删除',
  `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_merchant_type` (`merchant_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户信息表';

-- -------------------------------------------------------------
-- 13. biz_loan_application 贷款申请表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `biz_loan_application`;
CREATE TABLE `biz_loan_application` (
  `id`                    BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `apply_no`              VARCHAR(50)    NOT NULL COMMENT '申请编号',
  `user_id`               BIGINT         NOT NULL COMMENT '申请人ID',
  `loan_type`             VARCHAR(20)    NOT NULL COMMENT '贷款类型：A_TYPE/B_TYPE（A类5万循环/B类小额定向）',
  `apply_amount`          DECIMAL(18,2)  NULL COMMENT '申请金额',
  `purpose`               VARCHAR(200)   NULL COMMENT '贷款用途',
  `business_plan`         TEXT           NULL COMMENT '创业计划描述',
  `crowd_type`            VARCHAR(20)    NULL COMMENT '人群资质（冗余）',
  `pre_check_result`      VARCHAR(20)    NULL COMMENT '预审结果：ELIGIBLE/NOT_ELIGIBLE/NEED_MORE_INFO',
  `pre_check_min_amount`  DECIMAL(18,2)  NULL COMMENT '预审额度下限',
  `pre_check_max_amount`  DECIMAL(18,2)  NULL COMMENT '预审额度上限',
  `pre_check_detail`      TEXT           NULL COMMENT '预审详情（JSON）',
  `apply_status`          VARCHAR(30)    NOT NULL DEFAULT 'PRE_CHECK' COMMENT '申请状态：PRE_CHECK/PENDING_APPROVAL/APPROVED/REJECTED/CANCELLED',
  `approve_amount`        DECIMAL(18,2)  NULL COMMENT '审批金额',
  `reject_reason`         VARCHAR(500)   NULL COMMENT '拒绝原因',
  `submit_time`           DATETIME       NULL COMMENT '提交时间',
  `approve_time`          DATETIME       NULL COMMENT '审批时间',
  `deleted`               TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0存在 1删除',
  `create_time`           DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`           DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_apply_no` (`apply_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_loan_type` (`loan_type`),
  KEY `idx_apply_status` (`apply_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='贷款申请表';

-- -------------------------------------------------------------
-- 14. biz_credit_limit 授信额度表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `biz_credit_limit`;
CREATE TABLE `biz_credit_limit` (
  `id`              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`         BIGINT         NOT NULL COMMENT '用户ID',
  `credit_type`     VARCHAR(20)    NOT NULL COMMENT '授信类型：A_TYPE/B_TYPE',
  `total_limit`     DECIMAL(18,2)  NOT NULL COMMENT '总额度',
  `used_limit`      DECIMAL(18,2)  NOT NULL DEFAULT 0 COMMENT '已用额度',
  `available_limit` DECIMAL(18,2)  NOT NULL COMMENT '可用额度',
  `interest_rate`   DECIMAL(5,4)   NULL COMMENT '利率（年化）',
  `status`          VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/FROZEN/CLOSED',
  `effective_date`  DATE           NULL COMMENT '生效日期',
  `expire_date`     DATE           NULL COMMENT '到期日期',
  `observation_status` VARCHAR(20) NULL COMMENT '观察期状态：OBSERVING/PROMOTED/EXITED（B转A专用）',
  `observation_start`  DATE        NULL COMMENT '观察期开始日期',
  `observation_months` INT         NULL DEFAULT 0 COMMENT '已观察月数',
  `observation_score`  INT         NULL DEFAULT 0 COMMENT '观察期累计评分',
  `deleted`         TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0存在 1删除',
  `create_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_type` (`user_id`, `credit_type`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_credit_type` (`credit_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='授信额度表';

-- -------------------------------------------------------------
-- 15. biz_entrust_payment 受托支付表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `biz_entrust_payment`;
CREATE TABLE `biz_entrust_payment` (
  `id`                  BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `payment_no`          VARCHAR(50)    NOT NULL COMMENT '支付编号',
  `user_id`             BIGINT         NOT NULL COMMENT '借款人ID',
  `loan_application_id` BIGINT         NOT NULL COMMENT '贷款申请ID',
  `merchant_id`         BIGINT         NOT NULL COMMENT '收款商户ID',
  `merchant_name`       VARCHAR(200)   NOT NULL COMMENT '商户名称（冗余）',
  `amount`              DECIMAL(18,2)  NOT NULL COMMENT '支付金额',
  `purpose`             VARCHAR(200)   NULL COMMENT '用途说明',
  `trade_proof`         VARCHAR(500)   NULL COMMENT '交易凭证URL',
  `payment_status`      VARCHAR(20)    NOT NULL DEFAULT 'PENDING' COMMENT '支付状态：PENDING/PROCESSING/SUCCESS/FAILED',
  `payment_time`        DATETIME       NULL COMMENT '支付完成时间',
  `fail_reason`         VARCHAR(500)   NULL COMMENT '失败原因',
  `deleted`             TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0存在 1删除',
  `create_time`         DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`         DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_no` (`payment_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_payment_status` (`payment_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='受托支付表';

-- =============================================================
-- 五、经营赋能（2张）
-- =============================================================

-- -------------------------------------------------------------
-- 16. biz_bookkeeping_record 记账记录表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `biz_bookkeeping_record`;
CREATE TABLE `biz_bookkeeping_record` (
  `id`            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`       BIGINT         NOT NULL COMMENT '用户ID',
  `record_type`   VARCHAR(20)    NOT NULL COMMENT '记录类型：INCOME/EXPENSE',
  `category`      VARCHAR(50)    NOT NULL COMMENT '分类：收入/支出子类',
  `amount`        DECIMAL(18,2)  NOT NULL COMMENT '金额',
  `happen_date`   DATE           NOT NULL COMMENT '发生日期',
  `description`   VARCHAR(500)   NULL COMMENT '描述',
  `source`        VARCHAR(20)    NOT NULL DEFAULT 'MANUAL' COMMENT '来源：AUTO/MANUAL/IMPORT',
  `is_confirmed`  TINYINT        NOT NULL DEFAULT 1 COMMENT '是否已确认（模糊交易0=待确认）',
  `related_party` VARCHAR(100)   NULL COMMENT '交易对方',
  `deleted`       TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0存在 1删除',
  `create_time`   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_record_type` (`record_type`),
  KEY `idx_category` (`category`),
  KEY `idx_happen_date` (`happen_date`),
  KEY `idx_user_date` (`user_id`, `happen_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='记账记录表';

-- -------------------------------------------------------------
-- 17. biz_cashflow_report 现金流报表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `biz_cashflow_report`;
CREATE TABLE `biz_cashflow_report` (
  `id`             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`        BIGINT         NOT NULL COMMENT '用户ID',
  `report_period`  VARCHAR(20)    NOT NULL COMMENT '报表周期：2024-01 等',
  `total_income`   DECIMAL(18,2)  NOT NULL DEFAULT 0 COMMENT '总收入',
  `total_expense`  DECIMAL(18,2)  NOT NULL DEFAULT 0 COMMENT '总支出',
  `net_cash_flow`  DECIMAL(18,2)  NOT NULL COMMENT '净现金流',
  `profit_amount`  DECIMAL(18,2)  NULL COMMENT '利润额',
  `profit_margin`  DECIMAL(5,2)   NULL COMMENT '利润率%',
  `warning_level`  VARCHAR(20)    NULL COMMENT '预警等级：NORMAL/WARNING/CRITICAL',
  `warning_content` TEXT          NULL COMMENT '预警内容',
  `report_data`    TEXT           NULL COMMENT '详细报表数据（JSON）',
  `generate_time`  DATETIME       NULL COMMENT '生成时间',
  `deleted`        TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0存在 1删除',
  `create_time`    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_period` (`user_id`, `report_period`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_report_period` (`report_period`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='现金流报表';

-- =============================================================
-- 六、预算消费（4张）
-- =============================================================

-- -------------------------------------------------------------
-- 18. biz_budget_category 预算分类表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `biz_budget_category`;
CREATE TABLE `biz_budget_category` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `category_code` VARCHAR(50)  NOT NULL COMMENT '分类编码',
  `category_name` VARCHAR(50)  NOT NULL COMMENT '分类名称',
  `category_type` VARCHAR(20)  NOT NULL COMMENT '分类类型：FOOD/ENTERTAINMENT/SHOPPING/TRANSPORT/OTHER',
  `icon`          VARCHAR(100) NULL COMMENT '图标',
  `sort_order`    INT          NOT NULL DEFAULT 0 COMMENT '排序',
  `is_system`     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否系统预设：0否 1是',
  `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1正常',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_code` (`category_code`),
  KEY `idx_category_type` (`category_type`),
  KEY `idx_is_system` (`is_system`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预算分类表';

-- -------------------------------------------------------------
-- 19. biz_budget_setting 预算设置表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `biz_budget_setting`;
CREATE TABLE `biz_budget_setting` (
  `id`                BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`           BIGINT         NOT NULL COMMENT '用户ID',
  `category_id`       BIGINT         NOT NULL COMMENT '预算分类ID',
  `category_code`     VARCHAR(50)    NOT NULL COMMENT '分类编码（冗余）',
  `budget_period`     VARCHAR(20)    NOT NULL COMMENT '预算周期：2024-01 等',
  `budget_amount`     DECIMAL(18,2)  NOT NULL COMMENT '预算金额',
  `used_amount`       DECIMAL(18,2)  NOT NULL DEFAULT 0 COMMENT '已用金额',
  `remaining_amount`  DECIMAL(18,2)  NOT NULL COMMENT '剩余金额',
  `usage_percent`     DECIMAL(5,2)   NOT NULL DEFAULT 0 COMMENT '使用百分比',
  `remind_50_sent`    TINYINT        NOT NULL DEFAULT 0 COMMENT '50%提醒已发送',
  `remind_20_sent`    TINYINT        NOT NULL DEFAULT 0 COMMENT '20%提醒已发送',
  `remind_over_sent`  TINYINT        NOT NULL DEFAULT 0 COMMENT '超支提醒已发送',
  `deleted`           TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0存在 1删除',
  `create_time`       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_cat_period` (`user_id`, `category_id`, `budget_period`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_user_period` (`user_id`, `budget_period`),
  KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预算设置表';

-- -------------------------------------------------------------
-- 20. biz_transaction 交易记录表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `biz_transaction`;
CREATE TABLE `biz_transaction` (
  `id`               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`          BIGINT         NOT NULL COMMENT '用户ID',
  `transaction_no`   VARCHAR(50)    NOT NULL COMMENT '交易流水号',
  `transaction_type` VARCHAR(20)    NOT NULL COMMENT '交易类型：INCOME/EXPENSE',
  `category_id`      BIGINT         NULL COMMENT '分类ID（MCC映射后）',
  `category_code`    VARCHAR(50)    NULL COMMENT '分类编码（冗余）',
  `amount`           DECIMAL(18,2)  NOT NULL COMMENT '交易金额',
  `merchant_name`    VARCHAR(200)   NULL COMMENT '商户名称',
  `mcc_code`         VARCHAR(10)    NULL COMMENT 'MCC码',
  `transaction_time` DATETIME       NOT NULL COMMENT '交易时间',
  `description`      VARCHAR(500)   NULL COMMENT '交易描述',
  `source`           VARCHAR(20)    NOT NULL DEFAULT 'SIMULATED' COMMENT '来源：SIMULATED/BANK_IMPORT',
  `budget_id`        BIGINT         NULL COMMENT '关联预算ID',
  `deleted`          TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0存在 1删除',
  `create_time`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_transaction_no` (`transaction_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_transaction_time` (`transaction_time`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_user_time` (`user_id`, `transaction_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易记录表';

-- -------------------------------------------------------------
-- 21. biz_saving_goal 心愿储蓄表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `biz_saving_goal`;
CREATE TABLE `biz_saving_goal` (
  `id`               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`          BIGINT         NOT NULL COMMENT '用户ID',
  `goal_name`        VARCHAR(100)   NOT NULL COMMENT '目标名称',
  `target_amount`    DECIMAL(18,2)  NOT NULL COMMENT '目标金额',
  `current_amount`   DECIMAL(18,2)  NOT NULL DEFAULT 0 COMMENT '当前金额',
  `progress_percent` DECIMAL(5,2)   NOT NULL DEFAULT 0 COMMENT '进度百分比',
  `deadline`         DATE           NULL COMMENT '目标截止日期',
  `description`      VARCHAR(500)   NULL COMMENT '描述',
  `status`           VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/COMPLETED/CANCELLED',
  `deleted`          TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0存在 1删除',
  `create_time`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='心愿储蓄表';

-- =============================================================
-- 七、金融安全（4张）
-- =============================================================

-- -------------------------------------------------------------
-- 22. biz_anti_fraud_content 反诈内容表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `biz_anti_fraud_content`;
CREATE TABLE `biz_anti_fraud_content` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title`        VARCHAR(200) NOT NULL COMMENT '内容标题',
  `content_type` VARCHAR(20)  NOT NULL COMMENT '类型：ARTICLE/VIDEO/CASE',
  `category`     VARCHAR(30)  NULL COMMENT '分类：电信诈骗/网络诈骗/征信修复/套路贷 等',
  `summary`      VARCHAR(500) NULL COMMENT '摘要',
  `content`      TEXT         NOT NULL COMMENT '正文内容',
  `cover_image`  VARCHAR(500) NULL COMMENT '封面图',
  `view_count`   INT          NOT NULL DEFAULT 0 COMMENT '浏览量',
  `sort_order`   INT          NOT NULL DEFAULT 0 COMMENT '排序',
  `status`       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0下架 1发布',
  `publish_time` DATETIME     NULL COMMENT '发布时间',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`),
  KEY `idx_content_type` (`content_type`),
  KEY `idx_status` (`status`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='反诈内容表';

-- -------------------------------------------------------------
-- 23. biz_fraud_detection_log 骗局甄别记录表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `biz_fraud_detection_log`;
CREATE TABLE `biz_fraud_detection_log` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`        BIGINT        NOT NULL COMMENT '用户ID',
  `input_text`     TEXT          NOT NULL COMMENT '输入的话术文本',
  `detect_result`  VARCHAR(20)   NOT NULL COMMENT '检测结果：SAFE/SUSPICIOUS/DANGEROUS',
  `risk_level`     INT           NULL COMMENT '风险等级：1-5',
  `matched_rules`  TEXT          NULL COMMENT '命中的规则（JSON数组）',
  `warning_content` VARCHAR(1000) NULL COMMENT '警示内容',
  `detect_time`    DATETIME      NOT NULL COMMENT '检测时间',
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_detect_result` (`detect_result`),
  KEY `idx_detect_time` (`detect_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='骗局甄别记录表';

-- -------------------------------------------------------------
-- 24. biz_credit_report 征信报告表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `biz_credit_report`;
CREATE TABLE `biz_credit_report` (
  `id`                BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`           BIGINT         NOT NULL COMMENT '用户ID',
  `report_no`         VARCHAR(50)    NOT NULL COMMENT '报告编号',
  `report_type`       VARCHAR(20)    NOT NULL COMMENT '报告类型：SIMPLE/DETAIL',
  `credit_score`      INT            NULL COMMENT '信用分',
  `credit_level`      VARCHAR(20)    NULL COMMENT '信用等级：EXCELLENT/GOOD/FAIR/POOR',
  `total_loan_count`  INT            NULL COMMENT '贷款笔数',
  `overdue_count`     INT            NULL COMMENT '逾期笔数',
  `total_credit_limit` DECIMAL(18,2) NULL COMMENT '总授信额度',
  `used_credit_limit`  DECIMAL(18,2) NULL COMMENT '已用额度',
  `query_count`       INT            NULL COMMENT '查询次数',
  `report_summary`    VARCHAR(1000)  NULL COMMENT '报告摘要',
  `report_detail`     TEXT           NULL COMMENT '详细报告（JSON，演示数据）',
  `query_time`        DATETIME       NULL COMMENT '查询时间',
  `source`            VARCHAR(20)    NOT NULL DEFAULT 'SIMULATED' COMMENT '数据来源：SIMULATED/REAL',
  `deleted`           TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0存在 1删除',
  `create_time`       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_report_no` (`report_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_report_type` (`report_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='征信报告表';

-- -------------------------------------------------------------
-- 25. biz_risk_warning 风险预警表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `biz_risk_warning`;
CREATE TABLE `biz_risk_warning` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`         BIGINT        NOT NULL COMMENT '用户ID',
  `warning_type`    VARCHAR(30)   NOT NULL COMMENT '预警类型：OVERDUE_RISK/HIGH_FREQ_BORROW/CREDIT_ABNORMAL/BUDGET_OVER/CASHFLOW_WARNING',
  `warning_level`   VARCHAR(20)   NOT NULL COMMENT '预警等级：LOW/MEDIUM/HIGH/CRITICAL',
  `warning_title`   VARCHAR(200)  NOT NULL COMMENT '预警标题',
  `warning_content` TEXT          NOT NULL COMMENT '预警内容',
  `related_module`  VARCHAR(30)   NULL COMMENT '关联模块',
  `related_id`      BIGINT        NULL COMMENT '关联业务ID',
  `is_read`         TINYINT       NOT NULL DEFAULT 0 COMMENT '是否已读',
  `is_handled`      TINYINT       NOT NULL DEFAULT 0 COMMENT '是否已处理',
  `handle_note`     VARCHAR(500)  NULL COMMENT '处理备注',
  `warning_time`    DATETIME      NOT NULL COMMENT '预警时间',
  `handle_time`     DATETIME      NULL COMMENT '处理时间',
  `deleted`         TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0存在 1删除',
  `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_warning_type` (`warning_type`),
  KEY `idx_warning_level` (`warning_level`),
  KEY `idx_user_read` (`user_id`, `is_read`),
  KEY `idx_warning_time` (`warning_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险预警表';

-- =============================================================
-- 六、政策库（补充计划步骤2新增）
-- =============================================================

-- -------------------------------------------------------------
-- 26. biz_policy 政策库
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `biz_policy`;
CREATE TABLE `biz_policy` (
  `id`              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `policy_no`       VARCHAR(50)    NOT NULL COMMENT '政策编号',
  `policy_name`     VARCHAR(200)   NOT NULL COMMENT '政策名称',
  `policy_type`     VARCHAR(30)    NOT NULL COMMENT '政策类型：HOUSING-安居/ENTREPRENEUR-创业贴息',
  `target_crowd`    VARCHAR(200)   NOT NULL COMMENT '适用人群标签（逗号分隔）：STUDENT,GRADUATE,ENTREPRENEUR,OTHER',
  `max_amount`      DECIMAL(18,2)  NULL COMMENT '最高补贴/贷款额度',
  `subsidy_rate`    VARCHAR(50)    NULL COMMENT '贴息比例/补贴标准',
  `conditions`      TEXT           NULL COMMENT '申报条件速览',
  `apply_url`       VARCHAR(500)   NULL COMMENT '申报入口URL（模拟）',
  `policy_source`   VARCHAR(200)   NULL COMMENT '政策来源',
  `status`          VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/INACTIVE',
  `sort_order`      INT            NULL DEFAULT 0 COMMENT '排序',
  `deleted`         TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_policy_no` (`policy_no`),
  KEY `idx_policy_type` (`policy_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='政策库';

-- 演示数据：人才安居 4 条 + 创业贴息 4 条
INSERT INTO `biz_policy` (`policy_no`, `policy_name`, `policy_type`, `target_crowd`, `max_amount`, `subsidy_rate`, `conditions`, `apply_url`, `policy_source`, `sort_order`) VALUES
('ZABZ-001', '人才公寓申请', 'HOUSING', 'STUDENT,GRADUATE', 0.00, '租金减免30%-50%', '本科及以上学历；毕业5年内；在本地就业且社保满3月；无自有住房', '/api/v1/policy/apply/1', '市人力资源和社会保障局', 1),
('ZABZ-002', '青年落户补贴', 'HOUSING', 'GRADUATE', 30000.00, '一次性落户补贴最高3万', '全日制本科及以上应届毕业生；在本地企业就业；社保连续满6月', '/api/v1/policy/apply/2', '市公安局', 2),
('ZABZ-003', '应届毕业生租房补贴', 'HOUSING', 'GRADUATE', 12000.00, '每月1000元最长12月', '毕业2年内；在本地首次就业；家庭人均收入低于上年度人均可支配收入1.5倍', '/api/v1/policy/apply/3', '市住建局', 3),
('ZABZ-004', '新就业大学生住房保障', 'HOUSING', 'STUDENT,GRADUATE', 0.00, '公租房优先配租', '大专及以上学历；毕业未满5年；在本地稳定就业；人均住房面积低于16㎡', '/api/v1/policy/apply/4', '市住房保障中心', 4),
('CYTX-001', '创业担保贷款（个人最高30万）', 'ENTREPRENEUR', 'ENTREPRENEUR,GRADUATE', 300000.00, '财政贴息最高3%（LPR-150BP以内部分）', '法定劳动年龄内；有具体经营项目；信用良好；参加过创业培训', '/api/v1/policy/apply/5', '市人社局创业指导科', 5),
('CYTX-002', '创业担保贷款（部分地区最高50万）', 'ENTREPRENEUR', 'ENTREPRENEUR', 500000.00, '财政贴息LPR-150BP以内部分', '创业担保贷款优质项目；经营满1年；带动就业5人以上；部分地区试点', '/api/v1/policy/apply/6', '市人社局创业指导科', 6),
('CYTX-003', '个体工商户税费减免', 'ENTREPRENEUR', 'ENTREPRENEUR', 0.00, '月销售额10万以内免征增值税', '办理个体工商户营业执照；月销售额不超过10万元；小规模纳税人', '/api/v1/policy/apply/7', '市税务局', 7),
('CYTX-004', '青年创业场地补贴', 'ENTREPRENEUR', 'ENTREPRENEUR,GRADUATE', 18000.00, '每年最高6000元最长3年', '毕业5年内青年；首次创办经营实体；入驻认定的创业孵化基地', '/api/v1/policy/apply/8', '市科技局', 8);

-- =============================================================
-- 表数量校验：共 27 张表（原25 + biz_policy + biz_credit_txn）
-- =============================================================

-- -------------------------------------------------------------
-- 27. biz_credit_txn 循环贷交易流水（补充计划步骤3新增）
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `biz_credit_txn`;
CREATE TABLE `biz_credit_txn` (
  `id`               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `txn_no`           VARCHAR(50)    NOT NULL COMMENT '交易编号',
  `user_id`          BIGINT         NOT NULL COMMENT '用户ID',
  `credit_limit_id`  BIGINT         NOT NULL COMMENT '授信额度ID',
  `txn_type`         VARCHAR(20)    NOT NULL COMMENT '交易类型：WITHDRAW-提款/REPAY-还款',
  `principal_amount` DECIMAL(18,2)  NOT NULL COMMENT '本金金额',
  `interest_amount`  DECIMAL(18,2)  NOT NULL DEFAULT 0 COMMENT '利息金额（还款时计算）',
  `borrow_days`      INT            NOT NULL DEFAULT 0 COMMENT '计息天数（还款时计算）',
  `balance_after`    DECIMAL(18,2)  NULL COMMENT '交易后可用额度',
  `remark`           VARCHAR(500)   NULL COMMENT '备注',
  `txn_time`         DATETIME       NOT NULL COMMENT '交易时间',
  `deleted`          TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_txn_no` (`txn_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_credit_limit_id` (`credit_limit_id`),
  KEY `idx_txn_type` (`txn_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='循环贷交易流水';

-- =============================================================
-- 八、保险代销产品库（补充计划步骤4新增）
-- =============================================================

-- -------------------------------------------------------------
-- 28. biz_insurance_product 保险代销产品表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `biz_insurance_product`;
CREATE TABLE `biz_insurance_product` (
  `id`                BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `product_code`      VARCHAR(50)    NOT NULL COMMENT '产品编码',
  `product_name`      VARCHAR(200)   NOT NULL COMMENT '产品名称',
  `insurance_type`    VARCHAR(30)    NOT NULL COMMENT '险种：PERFORMANCE_BOND-履约保证/IP_PATENT-专利执行/IP_INFRINGEMENT-侵权责任/PROPERTY-财产综合',
  `scene`             VARCHAR(30)    NOT NULL COMMENT '经营场景标签：CONTRACT-合同履约/IP-知识产权/PROPERTY-财产/EMPLOYER-雇主',
  `target_crowd`      VARCHAR(200)   NULL COMMENT '适用人群标签：STUDENT,GRADUATE,ENTREPRENEUR,OTHER',
  `premium_rate`      VARCHAR(100)   NULL COMMENT '保费费率描述（模拟）',
  `coverage_amount`   DECIMAL(18,2)  NULL COMMENT '保额上限（模拟）',
  `insurer`           VARCHAR(200)   NOT NULL COMMENT '承保机构（模拟）',
  `product_elements`  TEXT           NULL COMMENT '产品要素速览',
  `conditions`        TEXT           NULL COMMENT '投保条件',
  `apply_url`         VARCHAR(500)   NULL COMMENT '模拟跳转投保入口URL',
  `status`            VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/INACTIVE',
  `sort_order`        INT            NULL DEFAULT 0 COMMENT '排序',
  `deleted`           TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time`       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_code` (`product_code`),
  KEY `idx_insurance_type` (`insurance_type`),
  KEY `idx_scene` (`scene`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='保险代销产品表（模拟）';

-- 演示数据：4 条模拟保险产品（补充计划步骤4 缺口 #24）
-- 合规口径：工行仅代销、不承保；演示用，不构成真实投保邀约
INSERT INTO `biz_insurance_product` (`product_code`, `product_name`, `insurance_type`, `scene`, `target_crowd`, `premium_rate`, `coverage_amount`, `insurer`, `product_elements`, `conditions`, `apply_url`, `sort_order`) VALUES
('BX-001', '履约保证保险（合同履约版）', 'PERFORMANCE_BOND', 'CONTRACT', 'ENTREPRENEUR,GRADUATE', '保费的1.5%-3%（按合同金额分档）', 500000.00, '工银安盛人寿（模拟）', '险种：履约保证保险；保障范围：被保险人未按合同履约时赔付合同金额；保险期间：与合同期一致；免赔额：合同金额的5%；投保主体：承包方/供应商', '签订正式经营合同；合同金额≥5万元；投保人信用良好；经营满6个月', '/api/v1/insurance/apply-demo/1', 1),
('BX-002', '专利执行保险（科技创业版）', 'IP_PATENT', 'IP', 'ENTREPRENEUR', '保费的5%-8%（按专利数量分档）', 200000.00, '工银安盛人寿（模拟）', '险种：知识产权保险-专利执行；保障范围：专利维权诉讼律师费、调查费、取证费；保险期间：1年；免赔额：5000元；投保主体：专利权人', '持有有效发明专利或实用新型专利；专利权属无争议；投保前未发生诉讼；经营满1年', '/api/v1/insurance/apply-demo/2', 2),
('BX-003', '侵权责任保险（被诉风险版）', 'IP_INFRINGEMENT', 'IP', 'ENTREPRENEUR', '保费的3%-6%（按营业收入分档）', 300000.00, '工银安盛人寿（模拟）', '险种：知识产权保险-侵权责任；保障范围：被诉侵权时承担的赔偿责任、应诉费用；保险期间：1年；免赔额：1万元；投保主体：经营者', '正常经营满1年；近2年无重大侵权诉讼；产品/服务有明确品类；投保人信用良好', '/api/v1/insurance/apply-demo/3', 3),
('BX-004', '小微企业财产综合险', 'PROPERTY', 'PROPERTY', 'ENTREPRENEUR,OTHER', '保费的0.8%-1.5%（按资产估值分档）', 1000000.00, '工银安盛人寿（模拟）', '险种：财产综合险；保障范围：火灾、爆炸、自然灾害造成的财产损失；保险期间：1年；免赔额：2000元；投保主体：经营主体', '有固定经营场所；资产估值≥10万元；消防设施合规；经营满6个月', '/api/v1/insurance/apply-demo/4', 4);

-- =============================================================
-- 表数量校验：共 28 张表（原25 + biz_policy + biz_credit_txn + biz_insurance_product）
-- =============================================================

-- =============================================================
-- 九、理财匹配与风险测评（补充计划步骤5新增）
-- =============================================================

-- -------------------------------------------------------------
-- 29. biz_finance_product 理财产品表（仅低风险）
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `biz_finance_product`;
CREATE TABLE `biz_finance_product` (
  `id`                BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `product_code`      VARCHAR(50)    NOT NULL COMMENT '产品编码',
  `product_name`      VARCHAR(200)   NOT NULL COMMENT '产品名称',
  `product_type`      VARCHAR(30)    NOT NULL COMMENT '产品类型：SAVING_GOAL-心愿储蓄/CASH_MANAGEMENT-现金管理/SHORT_BOND-短债/FUND_DCA-基金定投/GOLD_ACCUM-积存金',
  `risk_level`        VARCHAR(10)    NOT NULL COMMENT '风险等级：R1-低风险/R2-中低风险（仅此两档）',
  `expected_return`   VARCHAR(50)    NULL COMMENT '预期年化收益率（模拟）',
  `min_amount`        DECIMAL(18,2)  NULL COMMENT '起购金额',
  `period`            VARCHAR(100)   NULL COMMENT '投资期限描述',
  `product_elements`  TEXT           NULL COMMENT '产品要素速览',
  `risk_disclosure`   TEXT           NOT NULL COMMENT '风险揭示文案',
  `apply_url`         VARCHAR(500)   NULL COMMENT '模拟购买入口URL',
  `target_risk_level` VARCHAR(100)   NOT NULL COMMENT '适配风险等级（逗号分隔）：CONSERVATIVE/STEADY/BALANCED',
  `status`            VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/INACTIVE',
  `sort_order`        INT            NULL DEFAULT 0 COMMENT '排序',
  `deleted`           TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time`       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_code` (`product_code`),
  KEY `idx_risk_level` (`risk_level`),
  KEY `idx_product_type` (`product_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='理财产品表（仅低风险，模拟）';

-- 演示数据：5 条低风险产品（补充计划步骤5 缺口 #7）
-- 合规口径：理财非存款、产品有风险、工行仅代销；演示用，不构成投资建议
INSERT INTO `biz_finance_product` (`product_code`, `product_name`, `product_type`, `risk_level`, `expected_return`, `min_amount`, `period`, `product_elements`, `risk_disclosure`, `apply_url`, `target_risk_level`, `sort_order`) VALUES
('LC-001', '心愿储蓄（定期存款模拟）', 'SAVING_GOAL', 'R1', '1.50%（模拟）', 100.00, '灵活存取', '类型：储蓄类模拟；起购：100元；存期：灵活；付息：到期还本付息；特点：本金安全、收益稳定（模拟）', '存款类产品受存款保险条例保护，本金安全。本演示为模拟收益，实际以银行挂牌利率为准。', '/api/v1/finance-product/apply-demo/1', 'CONSERVATIVE,STEADY,BALANCED', 1),
('LC-002', '现金管理类产品（模拟）', 'CASH_MANAGEMENT', 'R1', '2.30%（7日年化，模拟）', 1000.00, 'T+1 赎回', '类型：现金管理类；起购：1000元；赎回：T+1；特点：流动性好、收益波动小（模拟）', '本产品为净值型理财，不承诺保本，过往业绩不预示未来表现。投资有风险，理财非存款。', '/api/v1/finance-product/apply-demo/2', 'CONSERVATIVE,STEADY,BALANCED', 2),
('LC-003', '短债基金（模拟）', 'SHORT_BOND', 'R2', '3.20%（近一年年化，模拟）', 1000.00, '建议持有 6 个月以上', '类型：债券型基金；起购：1000元；期限：建议6个月以上；特点：主投短期债券，波动较低（模拟）', '基金产品不保本，净值波动可能带来短期浮亏。理财非存款，产品有风险，投资需谨慎。', '/api/v1/finance-product/apply-demo/3', 'STEADY,BALANCED', 3),
('LC-004', '基金定投（指数型模拟）', 'FUND_DCA', 'R2', '历史年化 5%-8%（模拟）', 100.00, '建议定投 3 年以上', '类型：指数基金定投；起购：100元/期；期限：建议3年以上；特点：分散时点、平摊成本（模拟）', '基金定投不保本，市场波动可能导致亏损。理财非存款，产品有风险，过往业绩不预示未来。', '/api/v1/finance-product/apply-demo/4', 'STEADY,BALANCED', 4),
('LC-005', '积存金（黄金定投模拟）', 'GOLD_ACCUM', 'R2', '随金价波动（模拟）', 100.00, '建议持有 1 年以上', '类型：黄金积存；起购：100元/期；期限：建议1年以上；特点：分散买入黄金、对抗通胀（模拟）', '黄金价格波动较大，积存金不保本不保息。理财非存款，产品有风险，投资需谨慎。', '/api/v1/finance-product/apply-demo/5', 'BALANCED', 5);

-- -------------------------------------------------------------
-- 30. biz_risk_assessment 风险测评记录表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `biz_risk_assessment`;
CREATE TABLE `biz_risk_assessment` (
  `id`              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`         BIGINT         NOT NULL COMMENT '用户ID',
  `assess_no`       VARCHAR(50)    NOT NULL COMMENT '测评编号',
  `answers`         TEXT           NOT NULL COMMENT '答案JSON：[{q:1,opt:"A",score:2},...]',
  `total_score`     INT            NOT NULL COMMENT '总得分（10-40）',
  `risk_level`      VARCHAR(20)    NOT NULL COMMENT '风险等级：CONSERVATIVE-保守/STEADY-稳健/BALANCED-平衡',
  `risk_level_name` VARCHAR(20)    NOT NULL COMMENT '风险等级中文名',
  `valid_until`     DATE           NOT NULL COMMENT '测评有效期（1年）',
  `is_latest`       TINYINT        NOT NULL DEFAULT 1 COMMENT '是否最新：1最新 0历史',
  `assess_time`     DATETIME       NOT NULL COMMENT '测评时间',
  `deleted`         TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_assess_no` (`assess_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_user_latest` (`user_id`, `is_latest`),
  KEY `idx_risk_level` (`risk_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险测评记录表';


-- -------------------------------------------------------------
-- 31. biz_scenario_practice 反诈对话演练记录表（L3 对话式演练，模拟）
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `biz_scenario_practice`;
CREATE TABLE `biz_scenario_practice` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `practice_no`    VARCHAR(50)  NOT NULL COMMENT '演练编号，如 PRAC20260927001',
  `user_id`        BIGINT       NOT NULL COMMENT '演练用户ID',
  `scenario_id`    BIGINT       NOT NULL COMMENT '情景ID（biz_anti_fraud_content.id，SCENARIO_DIALOG）',
  `scenario_title` VARCHAR(200) NULL COMMENT '情景标题（冗余）',
  `round_count`    INT          NOT NULL DEFAULT 0 COMMENT '已完成回合数',
  `result`         VARCHAR(20)  NOT NULL COMMENT '结果：SAFE识破/LURED被诱骗/TIMEOUT超时/FINISHED主动结束',
  `risk_score`     INT          NOT NULL DEFAULT 0 COMMENT '综合风险分 0-100（越低越安全）',
  `result_desc`    VARCHAR(800) NULL COMMENT '判定结论（AI复盘摘要）',
  `started_at`     DATETIME     NULL COMMENT '演练开始时间',
  `ended_at`       DATETIME     NULL COMMENT '演练结束时间',
  `deleted`        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_practice_no` (`practice_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_scenario_id` (`scenario_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='反诈对话演练记录表（演示，模拟）';

-- -------------------------------------------------------------
-- 32. biz_scenario_round 反诈对话演练回合明细表（L3 对话式演练，模拟）
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `biz_scenario_round`;
CREATE TABLE `biz_scenario_round` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `practice_id` BIGINT        NOT NULL COMMENT '演练ID（biz_scenario_practice.id）',
  `round_no`    INT           NOT NULL COMMENT '回合序号',
  `speaker`     VARCHAR(20)   NOT NULL COMMENT '发言方：FRAUD诈骗方/USER用户',
  `content`     VARCHAR(1000) NOT NULL COMMENT '发言内容',
  `safe_score`  INT           NULL COMMENT '本回合安全分 0-100（用户发言回合才有）',
  `hit_words`   VARCHAR(500)  NULL COMMENT '命中的词库标签（逗号分隔）',
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_practice_id` (`practice_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='反诈对话演练回合明细表（演示，模拟）';

-- =============================================================
-- 表数量校验：共 32 张表（原30 + biz_scenario_practice + biz_scenario_round）
-- =============================================================
