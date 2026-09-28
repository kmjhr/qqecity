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
  `apply_url`       VARCHAR(500)   NULL COMMENT '申报入口URL（真实官方申报链接）',
  `valid_from`      DATE           NULL COMMENT '政策生效日期',
  `valid_to`        DATE           NULL COMMENT '政策失效日期（NULL=长期有效）',
  `keyword_tags`    VARCHAR(500)   NULL COMMENT '匹配关键词（逗号分隔）',
  `region`          VARCHAR(30)    NULL COMMENT '地区（全国/省/直辖市）',
  `policy_summary`  VARCHAR(500)   NULL COMMENT '政策概要（卡片速览）',
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
INSERT INTO `biz_policy` (`policy_no`, `policy_name`, `policy_type`, `target_crowd`, `max_amount`, `subsidy_rate`, `conditions`, `apply_url`, `valid_from`, `valid_to`, `keyword_tags`, `region`, `policy_summary`, `policy_source`, `sort_order`) VALUES
(0x5a41425a2d303031, 0xe4babae6898de4b893e9a1b9e7a79fe8b581e4bd8fe688bfefbc88e9858de7a79fefbc89, 0x484f5553494e47, 0x53545544454e542c47524144554154452c454e5452455052454e455552, 0.00, 0xe5b882e59cbae7a79fe98791e7baa637e68a98efbc88e6a8a1e68b9fe58fa3e5be84efbc89, 0xe69cace7a791e58f8ae4bba5e4b88ae5ada6e58e86efbc9be6af95e4b89a35e5b9b4e58685efbc88e68896e59ca8e6a0a1e5a4a7e5ada6e7949fefbc89efbc9be59ca8e69dade5b0b1e4b89a2fe5889be4b89aefbc9be59ca8e69dade697a0e688bfefbc9be58f82e58aa0e9a1b9e79baee99b86e4b8ade9858de7a79f, 0x68747470733a2f2f66676a2e68616e677a686f752e676f762e636e2f636f6c2f636f6c313632303130362f6172742f323032352f6172745f35383161393964333564616534393062613461386330373731366464646232392e68746d6c, '2025-01-01', NULL, 0xe69cace7a7912ce6af95e4b89a35e5b9b4e586852ce59ca8e6a0a1e7949f2ce697a0e688bf2ce5b0b1e4b89a2ce5889be4b89a2ce7a4bee4bf9d, 0xe6b599e6b19f, 0xe99da2e59091e59ca8e69dade99d92e5b9b4e4babae6898de9858de7a79fefbc8ce7a79fe98791e7baa6e4b8bae5b882e59cbae4bbb737e68a98efbc88e6a8a1e68b9fe58fa3e5be84efbc89, 0xe69dade5b79ee5b882e4bd8fe688bfe4bf9de99a9ce5928ce688bfe4baa7e7aea1e79086e5b180, 1),
(0x5a41425a2d303032, 0xe696b0e5b0b1e4b89ae5a4a7e5ada6e7949fe7a79fe688bfe8a1a5e8b4b4, 0x484f5553494e47, 0x4752414455415445, 12000.00, 0xe6af8fe69c8831303030e58583e69c80e995bf3132e69c88efbc88e6a8a1e68b9fe58fa3e5be84efbc89, 0xe69cace7a791e58f8ae4bba5e4b88ae5ada6e58e86e5ba94e5b18ae6af95e4b89ae7949fefbc9be59ca8e69dade5b0b1e4b89ae68896e887aae4b8bbe5889be4b89aefbc9be8bf9ee7bbade7bcb4e7bab3e7a4bee4bf9d36e4b8aae69c88efbc9be59ca8e69dade697a0e688bf, 0x68747470733a2f2f66676a2e68616e677a686f752e676f762e636e2f636f6c2f636f6c313232393236353031352f6172742f323032352f6172745f64663031636232663336393834393836393437333736646461393665653464382e68746d6c, '2025-01-01', NULL, 0xe69cace7a7912ce5ba94e5b18a2ce6af95e4b89a32e5b9b4e586852ce7a4bee4bf9d2ce697a0e688bf2ce5b0b1e4b89a2ce5889be4b89a, 0xe6b599e6b19f, 0xe69cace7a791e5ba94e5b18ae6af95e4b89ae7949fe69da5e69dade5b0b1e4b89ae5889be4b89aefbc8ce8bf9ee7bbade7bcb4e7a4bee4bf9d36e4b8aae69c88e58fafe794b3e9a286e6af8fe69c8831303030e58583e7a79fe688bfe8a1a5e8b4b4, 0xe69dade5b79ee5b882e4bd8fe688bfe4bf9de99a9ce5928ce688bfe4baa7e7aea1e79086e5b180, 2),
(0x5a41425a2d303033, 0xe99d92e88db7e9a9bfe7ab99efbc88e6b182e8818ce5858de8b4b9e4bd8fe5aebfefbc89, 0x484f5553494e47, 0x53545544454e542c4752414455415445, 0.00, 0xe5858de8b4b9e4bd8fe5aebf37e5a4a9efbc88e6a8a1e68b9fe58fa3e5be84efbc89, 0xe69da5e69dade6b182e8818ce79a84e5ba94e5b18ae6af95e4b89ae7949f2fe59ca8e6a0a1e5a4a7e5ada6e7949fefbc9be4babae6898de69dade5b79ee5b08fe7a88be5ba8fe5ae9ee5908de8aea4e8af81, 0x68747470733a2f2f66676a2e68616e677a686f752e676f762e636e2f636f6c2f636f6c313232393236353031352f6172742f323032362f6172745f66373461626263313031383834393734616163643764616531353835613764302e68746d6c, '2025-01-01', NULL, 0xe5ba94e5b18a2ce59ca8e6a0a1e7949f2ce6b182e8818c2ce697a0e688bf2ce69da5e69dad, 0xe6b599e6b19f, 0xe69da5e69dade6b182e8818ce5ba94e5b18ae7949f2fe59ca8e6a0a1e7949fe58fafe5858de8b4b9e585a5e4bd8fe99d92e88db7e9a9bfe7ab99efbc8ce69c80e995bf37e5a4a9, 0xe69dade5b79ee5b882e4bd8fe688bfe4bf9de99a9ce5928ce688bfe4baa7e7aea1e79086e5b180, 3),
(0x5a41425a2d303034, 0xe696b0e5b0b1e4b89ae5a4a7e5ada6e7949fe4bd8fe688bfe4bf9de99a9cefbc88e585ace7a79fe688bfe4bc98e58588e9858de7a79fefbc89, 0x484f5553494e47, 0x53545544454e542c4752414455415445, 0.00, 0xe585ace7a79fe688bfe4bc98e58588e9858de7a79f, 0xe5a4a7e4b893e58f8ae4bba5e4b88ae5ada6e58e86efbc9be6af95e4b89ae69caae6bba135e5b9b4efbc9be59ca8e69cace59cb0e7a8b3e5ae9ae5b0b1e4b89aefbc9be4babae59d87e4bd8fe688bfe99da2e7a7afe4bd8ee4ba8e3136e38ea1, 0x68747470733a2f2f66676a2e68616e677a686f752e676f762e636e2f636f6c2f636f6c313232393236353031352f6172742f323032352f6172745f37313533306235336537376534633338613631333231306565623965643237622e68746d6c, '2025-01-01', NULL, 0xe5a4a7e4b8932ce6af95e4b89a35e5b9b4e586852ce697a0e688bf2ce5b0b1e4b89a2ce7a4bee4bf9d, 0xe6b599e6b19f, 0xe5a4a7e4b893e4bba5e4b88ae6af95e4b89a35e5b9b4e58685e7a8b3e5ae9ae5b0b1e4b89ae38081e4babae59d87e4bd8fe688bfe4b88de8b6b33136e38ea1e58fafe794b3e8afb7e585ace7a79fe688bfe4bc98e58588e9858de7a79f, 0xe69dade5b79ee5b882e4bd8fe688bfe4bf9de99a9ce5928ce688bfe4baa7e7aea1e79086e5b180, 4),
(0x5a41425a2d303035, 0xe6b182e8818ce5889be4b89ae8a1a5e8b4b4efbc88e4b880e6aca1e680a733303030e58583efbc89, 0x484f5553494e47, 0x53545544454e542c4752414455415445, 3000.00, 0xe4b880e6aca1e680a733303030e58583, 0xe6af95e4b89ae5ada6e5b9b4e4bd8ee4bf9d2fe5ada4e584bf2fe6ae8be796be2fe58aa9e5ada6e8b4b7e6acbe2fe884b1e8b4abe7ad89e59bb0e99abee6af95e4b89ae7949f, 0x68747470733a2f2f687273732e68616e677a686f752e676f762e636e2f6172742f323032332f322f312f6172745f313232393537313537365f313832393139392e68746d6c, '2025-01-01', NULL, 0xe4bd8ee4bf9d2ce5ada4e584bf2ce6ae8be796bee4baba2ce58aa9e5ada6e8b4b7e6acbe2ce884b1e8b4ab2ce6af95e4b89ae7949f, 0xe6b599e6b19f, 0xe6af95e4b89ae5ada6e5b9b4e59bb0e99abee5aeb6e5baade7ad89e585ade7b1bbe6af95e4b89ae7949fe58fafe4b880e6aca1e680a7e794b3e9a28633303030e58583, 0xe69dade5b79ee5b882e4babae58a9be8b584e6ba90e5928ce7a4bee4bc9ae4bf9de99a9ce5b180, 5),
(0x435954582d303031, 0xe4b8aae4babae5889be4b89ae68b85e4bf9de8b4b7e6acbeefbc88e69c80e9ab983530e4b887efbc89, 0x454e5452455052454e455552, 0x454e5452455052454e4555522c47524144554154452c53545544454e542c5645544552414e2c44495341424c45442c4641524d4552, 500000.00, 0xe9878de782b9e4babae7bea4e585a8e9a29de8b4b4e681afefbc88e6a8a1e68b9fe58fa3e5be84efbc89, 0xe59ca8e69dade799bbe8aeb0e6b3a8e5868c35e5b9b4e58685e7bb8fe890a5e5ae9ee4bd93efbc9be6b395e5ae9ae58ab3e58aa8e5b9b4e9be84e6aeb5efbc9be697a0e4b88de889afe4bfa1e794a8e8aeb0e5bd95, 0x68747470733a2f2f687273732e68616e677a686f752e676f762e636e2f636f6c2f636f6c313538373933362f6172742f323032352f6172745f66613436353863623838633534386139383839626531363363633932383439622e68746d6c, '2025-01-01', NULL, 0xe5889be4b89a2ce6b3a8e5868c35e5b9b4e586852ce6af95e4b89ae7949f2ce59ca8e6a0a1e7949f2ce5889be4b89ae880852ce697a0e4b88de889afe4bfa1e794a8, 0xe6b599e6b19f, 0xe59ca8e69dade5889be4b89ae88085e69c80e9ab98e58fafe794b3e8afb73530e4b887e58583e5889be4b89ae68b85e4bf9de8b4b7e6acbeefbc8ce9878de782b9e4babae7bea4e58fafe585a8e9a29de8b4b4e681af, 0xe69dade5b79ee5b882e4babae58a9be8b584e6ba90e5928ce7a4bee4bc9ae4bf9de99a9ce5b180, 6),
(0x435954582d303032, 0xe4b880e6aca1e680a7e5889be4b89ae8a1a5e8b4b4efbc8833303030e58583efbc89, 0x454e5452455052454e455552, 0x454e5452455052454e4555522c47524144554154452c53545544454e54, 3000.00, 0xe4b880e6aca1e680a733303030e58583, 0xe59ca8e6a0a1e7949fe68896e6af95e4b89a35e5b9b4e58685e6af95e4b89ae7949fe5889de6aca1e5889be58a9ee4bc81e4b89a2fe4b8aae4bd93efbc9be6ada3e5b8b8e7bb8fe890a536e4b8aae69c88e4bba5e4b88a, 0x68747470733a2f2f7777772e7869616f7368616e2e676f762e636e2f636f6c2f636f6c313232393732383534372f6172742f323032362f6172745f33333736363339623536373434353262623064376638623137323331303737382e68746d6c, '2025-01-01', NULL, 0xe5889be4b89a2ce5889de6aca1e5889be4b89a2ce6af95e4b89ae7949f2ce59ca8e6a0a1e7949f2ce7bb8fe890a536e4b8aae69c882ce4b8aae4bd93e5b7a5e59586e688b7, 0xe6b599e6b19f, 0xe59ca8e6a0a1e7949f2fe6af95e4b89a35e5b9b4e58685e5889de6aca1e5889be4b89ae5b9b6e6ada3e5b8b8e7bb8fe890a536e4b8aae69c88efbc8ce4b880e6aca1e680a7e8a1a5e8b4b433303030e58583, 0xe69dade5b79ee5b882e890a7e5b1b1e58cbae694bfe5ba9cefbc88e6b599e6b19fe694bfe58aa1e69c8de58aa1e7bd91e794b3e68aa5efbc89, 7),
(0x435954582d303033, 0xe4b8aae4bd93e5b7a5e59586e688b7e7a88ee8b4b9e5878fe5858d, 0x454e5452455052454e455552, 0x454e5452455052454e4555522c4f54484552, 0.00, 0xe69c88e99480e594aee9a29d3130e4b887e4bba5e58685e5858de5be81e5a29ee580bce7a88e, 0xe58a9ee79086e4b8aae4bd93e5b7a5e59586e688b7e890a5e4b89ae689a7e785a7efbc9be5b08fe8a784e6a8a1e7bab3e7a88ee4babaefbc9be69c88e99480e594aee9a29de4b88de8b685e8bf873130e4b887e58583, 0x68747470733a2f2f7777772e68616e677a686f752e676f762e636e2f6172742f323032332f322f32372f6172745f313232393234333337365f35393037353632302e68746d6c, '2025-01-01', NULL, 0xe4b8aae4bd93e5b7a5e59586e688b72ce5b08fe8a784e6a8a1e7bab3e7a88ee4baba2ce69c88e99480e594aee9a29d3130e4b887, 0xe6b599e6b19f, 0xe4b8aae4bd93e5b7a5e59586e688b7e69c88e99480e594aee9a29d3130e4b887e4bba5e58685e5858de5be81e5a29ee580bce7a88eefbc88e5b08fe8a784e6a8a1e7bab3e7a88ee4babaefbc89, 0xe59bbde5aeb6e7a88ee58aa1e680bbe5b1802fe69dade5b79ee5b882e694bfe5ba9c, 8),
(0x435954582d303034, 0xe5889be4b89ae999aae8b791e7a9bae997b4e59cbae59cb0e7a79fe98791e8a1a5e8b4b4, 0x454e5452455052454e455552, 0x454e5452455052454e4555522c47524144554154452c53545544454e54, 0.00, 0xe69c80e9ab9833e585832fe38ea1c2b7e5a4a9efbc8c3530e38ea1e4bba5e58685e69c80e995bf33e5b9b4efbc88e6a8a1e68b9fe58fa3e5be84efbc89, 0xe9878de782b9e4babae7bea4e5889be4b89ae88085e585a5e9a9bbe5889be4b89ae999aae8b791e7a9bae997b4e6bba136e4b8aae69c88efbc9be59ca8e69dade7bcb4e7bab3e7a4bee4bf9d, 0x68747470733a2f2f687273732e68616e677a686f752e676f762e636e2f636f6c2f636f6c313232393537313537362f6172742f323032352f6172745f30623835353366386132373264616162333761636431336666643032356335342e68746d6c, '2025-01-01', NULL, 0xe5889be4b89a2ce585a5e9a9bbe999aae8b791e7a9bae997b42ce7a4bee4bf9d2ce6af95e4b89ae7949f2ce59ca8e6a0a1e7949f, 0xe6b599e6b19f, 0xe585a5e9a9bbe5889be4b89ae999aae8b791e7a9bae997b4e6bba136e4b8aae69c88efbc8ce7a79fe98791e69c80e9ab98e8a1a533e585832fe38ea1c2b7e5a4a9efbc8c3530e38ea1e58685e69c80e995bf33e5b9b4, 0xe69dade5b79ee5b882e4babae58a9be8b584e6ba90e5928ce7a4bee4bc9ae4bf9de99a9ce5b180, 9),
(0x435954582d303035, 0xe4b880e6aca1e680a7e5889be4b89ae7a4bee4bf9de8a1a5e8b4b4efbc8835303030e58583efbc89, 0x454e5452455052454e455552, 0x454e5452455052454e4555522c47524144554154452c53545544454e54, 5000.00, 0xe4b880e6aca1e680a735303030e58583, 0xe6af95e4b89a35e5b9b4e58685e5889de6aca1e5889be58a9ee4bc81e4b89a2fe4b8aae4bd93efbc9be8bf9ee7bbade7bcb4e7bab3e7a4bee4bf9de6bba13132e4b8aae69c88, 0x68747470733a2f2f7777772e7869616f7368616e2e676f762e636e2f636f6c2f636f6c313232393732383534372f6172742f323032362f6172745f33333736363339623536373434353262623064376638623137323331303737382e68746d6c, '2025-01-01', NULL, 0xe5889be4b89a2ce7a4bee4bf9d3132e4b8aae69c882ce6af95e4b89ae7949f2ce59ca8e6a0a1e7949f2ce5889de6aca1e5889be4b89a, 0xe6b599e6b19f, 0xe6af95e4b89a35e5b9b4e58685e5889de6aca1e5889be4b89ae5b9b6e8bf9ee7bbade7bcb4e7a4bee4bf9d3132e4b8aae69c88efbc8ce4b880e6aca1e680a7e8a1a5e8b4b435303030e58583, 0xe69dade5b79ee5b882e890a7e5b1b1e58cbae694bfe5ba9cefbc88e6b599e6b19fe694bfe58aa1e69c8de58aa1e7bd91e794b3e68aa5efbc89, 10),
(0x435954582d303036, 0xe5a4a7e5ada6e7949fe5889be4b89ae9a1b9e79baee8b584e58aa9efbc88352d3230e4b887e69c80e9ab983530e4b887efbc89, 0x454e5452455052454e455552, 0x454e5452455052454e4555522c47524144554154452c53545544454e54, 500000.00, 0xe58cbae58ebfe5b882e8af84e5aea1352d3230e4b887efbc8ce5b882e7baa7e69c80e9ab983530e4b887, 0xe6af95e4b89a35e5b9b4e58685e6af95e4b89ae7949fe68896e59ca8e69dade9ab98e6a0a1e59ca8e6a0a1e7949fefbc9be696b0e5889be58a9ee4bc81e4b89aefbc9be8af84e5aea1e9809ae8bf87, 0x68747470733a2f2f687273732e68616e677a686f752e676f762e636e2f636f6c2f636f6c313538373937382f6172742f323032362f6172745f31306132626464663165656334383664396134333535313564663139366564322e68746d6c, '2025-01-01', NULL, 0xe5889be4b89a2ce6af95e4b89a35e5b9b4e586852ce59ca8e6a0a1e7949f2ce696b0e5889be58a9ee4bc81e4b89a2ce9a1b9e79baee8af84e5aea1, 0xe6b599e6b19f, 0xe5a4a7e5ada6e7949fe5889be4b89ae9a1b9e79baee7bb8fe8af84e5aea1efbc9ae58cbae58ebf352d3230e4b887efbc8ce5b882e7baa7e7bbbce59088e8af84e5aea1e69c80e9ab983530e4b887, 0xe69dade5b79ee5b882e4babae58a9be8b584e6ba90e5928ce7a4bee4bc9ae4bf9de99a9ce5b180, 11),
(0x435954582d303037, 0xe5889be4b89ae5b8a6e58aa8e5b0b1e4b89ae8a1a5e8b4b4, 0x454e5452455052454e455552, 0x454e5452455052454e4555522c4752414455415445, 0.00, 0xe68c89e5b8a6e58aa8e5b0b1e4b89ae4babae695b0e8a1a5e8b4b4efbc88e6a8a1e68b9fe58fa3e5be84efbc89, 0xe9878de782b9e4babae7bea4e5889be58a9ee4bc81e4b89a2fe4b8aae4bd93efbc9be5b8a6e58aa832e4babae4bba5e4b88ae5b0b1e4b89ae5b9b6e8bf9ee7bbade7bcb4e7a4bee4bf9de6bba13132e4b8aae69c88, 0x68747470733a2f2f7777772e68616e677a686f752e676f762e636e2f6172742f323032332f322f32372f6172745f313232393234333337365f35393037353632302e68746d6c, '2025-01-01', NULL, 0xe5889be4b89a2ce5b8a6e58aa8e5b0b1e4b89a2ce7a4bee4bf9d2ce6af95e4b89ae7949f, 0xe6b599e6b19f, 0xe5889be58a9ee4bc81e4b89ae5b8a6e58aa832e4babae4bba5e4b88ae5b0b1e4b89ae5b9b6e7bcb4e7a4bee4bf9de6bba13132e4b8aae69c88efbc8ce68c89e4babae695b0e7bb99e4ba88e8a1a5e8b4b4, 0xe69dade5b79ee5b882e4babae6b091e694bfe5ba9c, 12),
(0x47422d303031, 0xe4b880e6aca1e680a7e6b182e8818ce8a1a5e8b4b4efbc88e59bb0e99abee6af95e4b89ae7949fefbc89, 0x484f5553494e47, 0x53545544454e542c4752414455415445, 3000.00, 0xe59084e59cb0313030302d33303030e585832fe4baba, 0xe6af95e4b89ae5ada6e5b9b4e4bd8ee4bf9de5aeb6e5baad2fe99bb6e5b0b1e4b89ae5aeb6e5baad2fe998b2e6ada2e8bf94e8b4abe79b91e6b58be5afb9e8b1a1e5aeb6e5baad2fe789b9e59bb0e4babae591982fe6ae8be796be2fe58aa9e5ada6e8b4b7e6acbee9ab98e6a0a1e6af95e4b89ae7949f, 0x68747470733a2f2f7777772e676f762e636e2f7a68656e6763652f3230323530392f636f6e74656e745f373034323539322e68746d, '2025-01-01', NULL, 0xe4bd8ee4bf9d2ce99bb6e5b0b1e4b89a2ce789b9e59bb02ce6ae8be796bee4baba2ce58aa9e5ada6e8b4b7e6acbe2ce6af95e4b89ae7949f, 0xe585a8e59bbd, 0xe585ade7b1bbe59bb0e99abee9ab98e6a0a1e6af95e4b89ae7949fe6af95e4b89ae5ada6e5b9b4e58fafe794b3e9a286e4b880e6aca1e680a7e6b182e8818ce8a1a5e8b4b4efbc88e59084e59cb0313030302d33303030e58583efbc89, 0xe4b8ade59bbde694bfe5ba9ce7bd912fe4babae58a9be8b584e6ba90e7a4bee4bc9ae4bf9de99a9ce983a8, 13),
(0x47422d303032, 0xe4b880e6aca1e680a7e5889be4b89ae8a1a5e8b4b4efbc88e9a696e6aca1e5889be4b89aefbc89, 0x454e5452455052454e455552, 0x454e5452455052454e4555522c4752414455415445, 0.00, 0xe59084e59cb0353030302d3130303030e58583, 0xe7a6bbe6a0a132e5b9b4e58685e9ab98e6a0a1e6af95e4b89ae7949f2fe5b0b1e4b89ae59bb0e99abee4babae591982fe8bf94e4b9a1e585a5e4b9a1e5869ce6b091e5b7a5efbc9be9a696e6aca1e5889be58a9ee5b08fe5beaee4bc81e4b89ae68896e4b8aae4bd93efbc9be6ada3e5b8b8e8bf90e890a531e5b9b4e4bba5e4b88a, 0x68747470733a2f2f7777772e676f762e636e2f7a68656e6763652f3230323531302f636f6e74656e745f373034343537312e68746d, '2025-01-01', NULL, 0xe5889be4b89a2ce9a696e6aca1e5889be4b89a2ce6af95e4b89ae7949f2ce8bf90e890a531e5b9b42ce8bf94e4b9a1, 0xe585a8e59bbd, 0xe9a696e6aca1e5889be58a9ee5b08fe5beaee4bc81e4b89ae68896e4b8aae4bd93e5b9b6e6ada3e5b8b8e8bf90e890a531e5b9b4e4bba5e4b88aefbc8ce58fafe794b3e9a286e4b880e6aca1e680a7e5889be4b89ae8a1a5e8b4b4efbc88e59084e59cb0353030302d3130303030e58583efbc89, 0xe4b8ade59bbde694bfe5ba9ce7bd912fe4babae58a9be8b584e6ba90e7a4bee4bc9ae4bf9de99a9ce983a8, 14),
(0x47422d303033, 0xe8818ce4b89ae59fb9e8aeade8a1a5e8b4b4efbc88e5b0b1e4b89ae68a80e883bd2be5889be4b89ae59fb9e8aeadefbc89, 0x454e5452455052454e455552, 0x53545544454e542c47524144554154452c454e5452455052454e455552, 0.00, 0xe68c89e59fb9e8aeade8af81e4b9a6e7b1bbe588abe5ae9ae9a29de8a1a5e8b4b4, 0xe6af95e4b89ae5b9b4e5baa6e9ab98e6a0a1e6af95e4b89ae7949fe58f82e58aa0e5b0b1e4b89ae68a80e883bde59fb9e8aead2fe5889be4b89ae59fb9e8aeade5b9b6e58f96e5be97e8af81e4b9a6, 0x687474703a2f2f6e6577732e636e722e636e2f6e61746976652f67642f32303235303730342f7432303235303730345f3532373234343736312e7368746d6c, '2025-01-01', NULL, 0xe6af95e4b89ae7949f2ce59ca8e6a0a1e7949f2ce5889be4b89a2ce59fb9e8aead, 0xe585a8e59bbd, 0xe58f82e58aa0e5b0b1e4b89ae68a80e883bd2fe5889be4b89ae59fb9e8aeade5b9b6e58f96e5be97e8af81e4b9a6efbc8ce68c89e7b1bbe588abe7bb99e4ba88e8818ce4b89ae59fb9e8aeade8a1a5e8b4b4, 0xe5a4aee5b9bfe7bd912fe4babae58a9be8b584e6ba90e7a4bee4bc9ae4bf9de99a9ce983a8, 15),
(0x47422d303034, 0xe4b8aae4babae5889be4b89ae68b85e4bf9de8b4b7e6acbeefbc88e8b4a2e694bfe8b4b4e681afefbc89, 0x454e5452455052454e455552, 0x454e5452455052454e4555522c47524144554154452c53545544454e542c5645544552414e2c44495341424c45442c4641524d4552, 300000.00, 0xe588a9e78e874c50522b35304250e58685e68c89353025e8b4b4e681afefbc88e6a8a1e68b9fe58fa3e5be84efbc89, 0xe9ab98e6a0a1e6af95e4b89ae7949fe7ad89e5889be4b89ae88085e887aae4b8bbe5889be4b89ae8b584e98791e4b88de8b6b3efbc9be69c89e7bb8fe890a5e5ae9ee4bd93, 0x68747470733a2f2f7777772e676f762e636e2f7a68656e6763652f3230323531302f636f6e74656e745f373034343537312e68746d, '2025-01-01', NULL, 0xe5889be4b89a2ce6af95e4b89ae7949f2ce59ca8e6a0a1e7949f2ce5889be4b89ae88085, 0xe585a8e59bbd, 0xe887aae4b8bbe5889be4b89ae58fafe794b3e8afb7e5889be4b89ae68b85e4bf9de8b4b7e6acbeefbc88e4b8aae4babae69c80e9ab983330e4b887efbc89efbc8ce8b4b7e6acbee5ae9ee99985e588a9e78e87353025e8b4a2e694bfe8b4b4e681af, 0xe4babae58a9be8b584e6ba90e7a4bee4bc9ae4bf9de99a9ce983a8, 16),
(0x47422d303035, 0xe9ab98e6a0a1e6af95e4b89ae7949fe5889be4b89ae7a88ee8b4b9e689a3e5878f, 0x454e5452455052454e455552, 0x454e5452455052454e4555522c47524144554154452c53545544454e54, 0.00, 0x33e5b9b4e58685e6af8fe688b7e6af8fe5b9b43234303030e58583e99990e9a29d, 0xe6af95e4b89ae5b9b4e5baa6e58685e9ab98e6a0a1e6af95e4b89ae7949fe4bb8ee4ba8be4b8aae4bd93e7bb8fe890a5efbc9be58a9ee79086e890a5e4b89ae689a7e785a7, 0x68747470733a2f2f6a73687273732e6a69616e6773752e676f762e636e2f6d6f64756c652f646f776e6c6f61642f646f776e66696c652e6a73703f636c61737369643d302666696c656e616d653d39363133616365643339626234616435386537303638333832636339373663382e706466, '2025-01-01', NULL, 0xe5889be4b89a2ce4b8aae4bd93e7bb8fe890a52ce6af95e4b89ae7949f2ce59ca8e6a0a1e7949f, 0xe585a8e59bbd, 0xe6af95e4b89ae5b9b4e5baa6e58685e5889be4b89ae4bb8ee4ba8be4b8aae4bd93e7bb8fe890a5efbc8c33e5b9b4e58685e6af8fe688b7e6af8fe5b9b43234303030e58583e99990e9a29de689a3e5878fe7a88ee8b4b9, 0xe8b4a2e694bfe983a82fe7a88ee58aa1e680bbe5b1802fe4babae7a4bee983a8, 17),
(0x47422d303036, 0xe4b880e6aca1e680a7e689a9e5b297e8a1a5e58aa9efbc88e4bc81e4b89ae590b8e7bab3efbc89, 0x454e5452455052454e455552, 0x454e5452455052454e4555522c4752414455415445, 0.00, 0xe59084e59cb0313030302d31353030e585832fe4baba, 0xe4bc81e4b89ae68b9be794a8e6af95e4b89ae5b9b4e5baa6e58f8ae7a6bbe6a0a132e5b9b4e58685e69caae5b0b1e4b89ae9ab98e6a0a1e6af95e4b89ae7949fefbc8ce7adbee8aea2e59088e5908ce5b9b6e7bcb4e7a4bee4bf9d33e4b8aae69c88e4bba5e4b88a, 0x68747470733a2f2f6368726d2e6d6f687273732e676f762e636e2f3f706f73745f747970653d706f737426703d3134333439, '2025-01-01', NULL, 0xe5889be4b89a2ce6af95e4b89ae7949f2ce590b8e7bab3e5b0b1e4b89a, 0xe585a8e59bbd, 0xe4bc81e4b89ae68b9be794a8e6af95e4b89ae7949fe5b9b6e7bcb4e7a4bee4bf9d33e4b8aae69c88e4bba5e4b88aefbc8ce68c89e4babae695b0e7bb99e4ba88e4b880e6aca1e680a7e689a9e5b297e8a1a5e58aa9efbc88e59084e59cb0313030302d31353030e585832fe4babaefbc89, 0xe4b8ade59bbde4babae58a9be8b584e6ba90e5b882e59cbae7bd91, 18),
(0x47442d303031, 0xe4b8aae4babae5889be4b89ae68b85e4bf9de8b4b7e6acbeefbc88e69c80e9ab983630e4b887efbc89, 0x454e5452455052454e455552, 0x454e5452455052454e4555522c47524144554154452c53545544454e542c5645544552414e2c44495341424c45442c4641524d4552, 600000.00, 0xe588a9e78e87e4b88de8b6854c50522b35304250efbc8ce68c89353025e8b4b4e681af, 0xe59ca8e6a0a1e5a4a7e5ada6e7949fe58f8ae9ab98e6a0a1e6af95e4b89ae7949fe887aae4b8bbe5889be4b89aefbc9be5b8a6e58aa833e4babae4bba5e4b88ae5b0b1e4b89ae9a29de5baa6e68f90e887b33630e4b887, 0x68747470733a2f2f687273732e67642e676f762e636e2f7a646c7978782f7a787a792f636f6e74656e742f6d706f73745f343732353736312e68746d6c, '2025-01-01', NULL, 0xe5889be4b89a2ce6af95e4b89ae7949f2ce59ca8e6a0a1e7949f2ce5889be4b89ae88085, 0xe5b9bfe4b89c, 0xe5b9bfe4b89ce59ca8e6a0a1e7949f2fe6af95e4b89ae7949fe5889be4b89ae58fafe794b3e8afb7e5889be4b89ae68b85e4bf9de8b4b7e6acbeefbc8ce4b8aae4babae69c80e9ab983330e4b887e38081e5b8a6e58aa8e5b0b1e4b89ae69c80e9ab983630e4b887efbc8c353025e8b4b4e681af, 0xe5b9bfe4b89ce79c81e4babae58a9be8b584e6ba90e5928ce7a4bee4bc9ae4bf9de99a9ce58e85, 19),
(0x47442d303032, 0xe4b880e6aca1e680a7e5889be4b89ae8b584e58aa9, 0x454e5452455052454e455552, 0x454e5452455052454e4555522c47524144554154452c53545544454e542c5645544552414e, 10000.00, 0xe4b880e6aca1e680a73130303030e58583, 0xe59ca8e6a0a1e58f8ae6af95e4b89a35e5b9b4e58685e5ada6e7949f2fe98080e5bdb9e5869be4baba2fe799bbe8aeb0e5a4b1e4b89ae4babae59198e7ad89e5889de5889be88085efbc9be6ada3e5b8b8e7bb8fe890a536e4b8aae69c88e4bba5e4b88a, 0x68747470733a2f2f7777772e67647a7766772e676f762e636e2f706f7274616c2f76322f67756964652f31313434303830303535393137333833344e34343432313131304130303032, '2025-01-01', NULL, 0xe5889be4b89a2ce6af95e4b89ae7949f2ce59ca8e6a0a1e7949f2ce98080e5bdb9e5869be4baba2ce5889de6aca1e5889be4b89a, 0xe5b9bfe4b89c, 0xe59ca8e6a0a1e58f8ae6af95e4b89a35e5b9b4e58685e5ada6e7949fe7ad89e5889de5889be88085efbc8ce6ada3e5b8b8e7bb8fe890a536e4b8aae69c88e4bba5e4b88ae58fafe794b3e9a286e4b880e6aca1e680a7e5889be4b89ae8b584e58aa931e4b887e58583, 0xe5b9bfe4b89ce694bfe58aa1e69c8de58aa1e7bd91, 20),
(0x47442d303033, 0xe5b08fe5beaee4bc81e4b89ae5889be4b89ae68b85e4bf9de8b4b7e6acbeefbc88e69c80e9ab98353030e4b887efbc89, 0x454e5452455052454e455552, 0x454e5452455052454e455552, 5000000.00, 0xe68c89e8b4b7e6acbee5ae9ee99985e588a9e78e87353025e8b4b4e681af, 0xe59ca8e7b2a4e799bbe8aeb0e6b3a8e5868ce5b08fe5beaee4bc81e4b89aefbc9be696b0e68b9be794a8e9878de782b9e689b6e68c81e5afb9e8b1a1e8bebee8818ce5b7a5e4babae695b0313025e4bba5e4b88a, 0x68747470733a2f2f7777772e64672e676f762e636e2f68656e676c692f7a772f7a646c797878676b2f6a7963792f636f6e74656e742f706f73745f343437363030302e68746d6c, '2025-01-01', NULL, 0xe5889be4b89a2ce5b08fe5beaee4bc81e4b89a2ce590b8e7bab3e5b0b1e4b89a, 0xe5b9bfe4b89c, 0xe5b9bfe4b89ce5b08fe5beaee4bc81e4b89ae68b9be794a8e9878de782b9e7bea4e4bd93e8bebee6a087efbc8ce58fafe794b3e8afb7e69c80e9ab98353030e4b887e58583e5889be4b89ae68b85e4bf9de8b4b7e6acbe, 0xe4b89ce88e9ee5b882e4babae7a4bee5b180efbc88e5b9bfe4b89ce79c81e78eb0e8a18ce694bfe7ad96efbc89, 21),
(0x4a532d303031, 0xe4b880e6aca1e680a7e5889be4b89ae8a1a5e8b4b4, 0x454e5452455052454e455552, 0x454e5452455052454e4555522c47524144554154452c53545544454e54, 5000.00, 0xe4b880e6aca1e680a735303030e58583, 0xe9a696e6aca1e59ca8e88b8fe5889be58a9ee4bc81e4b89a2fe4b8aae4bd93efbc9be6ada3e5b8b8e7bb8fe890a536e4b8aae69c88e4bba5e4b88a, 0x68747470733a2f2f31323334352e6a737a7766772e676f762e636e2f636e732d626d66772d7773627364742f696e6465782f70616765732f64656661756c742f78737a777764742e68746d6c3f706174683d70726f62652d64657461696c2669643d31393039373738313237373133353632363235, '2025-01-01', NULL, 0xe5889be4b89a2ce5889de6aca1e5889be4b89a2ce6af95e4b89ae7949f2ce59ca8e6a0a1e7949f2ce7bb8fe890a536e4b8aae69c88, 0xe6b19fe88b8f, 0xe9a696e6aca1e59ca8e6b19fe88b8fe5889be4b89ae5b9b6e6ada3e5b8b8e7bb8fe890a536e4b8aae69c88e4bba5e4b88aefbc8ce4b880e6aca1e680a7e8a1a5e8b4b435303030e58583, 0xe6b19fe88b8f31323334352fe6b19fe88b8fe79c81e4babae7a4bee58e85, 22),
(0x4a532d303032, 0xe5889be4b89ae5b8a6e58aa8e5b0b1e4b89ae8a1a5e8b4b4, 0x454e5452455052454e455552, 0x454e5452455052454e4555522c4752414455415445, 20000.00, 0xe6af8fe4baba32303030e58583efbc8ce6af8fe5aeb6e69c80e9ab9832e4b887, 0xe5889be4b89ae5ae9ee4bd93e590b8e7bab3e585b6e4bb96e58ab3e58aa8e88085e5b0b1e4b89aefbc8ce7adbee8aea231e5b9b4e4bba5e4b88ae59088e5908ce5b9b6e7bcb4e7a4bee4bf9d36e4b8aae69c88e4bba5e4b88a, 0x68747470733a2f2f31323334352e6a737a7766772e676f762e636e2f636e732d626d66772d7773627364742f696e6465782f70616765732f64656661756c742f78737a777764742e68746d6c3f706174683d70726f62652d64657461696c2669643d31393039373738313237373133353632363235, '2025-01-01', NULL, 0xe5889be4b89a2ce5b8a6e58aa8e5b0b1e4b89a2ce7a4bee4bf9d, 0xe6b19fe88b8f, 0xe6b19fe88b8fe5889be4b89ae5ae9ee4bd93e590b8e7bab3e5b0b1e4b89aefbc8ce68c89e6af8fe4baba32303030e58583e8a1a5e8b4b4efbc8ce6af8fe5aeb6e69c80e9ab9832e4b887e58583, 0xe6b19fe88b8f31323334352fe6b19fe88b8fe79c81e4babae7a4bee58e85, 23),
(0x4a532d303033, 0xe5af8ce6b091e5889be4b89ae68b85e4bf9de8b4b7e6acbeefbc88e58d97e4baacefbc89, 0x454e5452455052454e455552, 0x454e5452455052454e4555522c4752414455415445, 500000.00, 0xe69c80e9ab98353025e8b4b4e681afefbc88e6a8a1e68b9fe58fa3e5be84efbc89, 0xe9a696e6aca1e59ca8e58d97e4baace5889be4b89aefbc9be58fafe58fa0e58aa032303030e58583e5bc80e4b89ae8a1a5e8b4b4, 0x687474703a2f2f7777772e6a732e78696e6875616e65742e636f6d2f32303235303832332f64373532386632623562616234386338393364366634653936323936363839312f632e68746d6c, '2025-01-01', NULL, 0xe5889be4b89a2ce6af95e4b89ae7949f2ce59ca8e6a0a1e7949f2ce9a696e6aca1e5889be4b89a, 0xe6b19fe88b8f, 0xe58d97e4baace9a696e6aca1e5889be4b89ae69c80e9ab98e58fafe8b4b73530e4b887e58583e5af8ce6b091e5889be4b89ae68b85e4bf9de8b4b7e6acbeefbc88353025e8b4b4e681afefbc89efbc8ce58fa6e4baab32303030e58583e5bc80e4b89ae8a1a5e8b4b4, 0xe696b0e58d8ee7bd91e6b19fe88b8fe9a291e98193, 24),
(0x4a532d303034, 0xe9ab98e6a0a1e6af95e4b89ae7949fe5889be4b89ae7a88ee8b4b9e5878fe5858d, 0x454e5452455052454e455552, 0x454e5452455052454e4555522c4752414455415445, 0.00, 0x33e5b9b4e58685e6af8fe688b7e6af8fe5b9b43234303030e58583e99990e9a29d, 0xe6af95e4b89ae5b9b4e5baa6e58685e5889be4b89ae4bb8ee4ba8be4b8aae4bd93e7bb8fe890a5, 0x68747470733a2f2f6a73687273732e6a69616e6773752e676f762e636e2f6d6f64756c652f646f776e6c6f61642f646f776e66696c652e6a73703f636c61737369643d302666696c656e616d653d39363133616365643339626234616435386537303638333832636339373663382e706466, '2025-01-01', NULL, 0xe5889be4b89a2ce4b8aae4bd93e7bb8fe890a52ce6af95e4b89ae7949f, 0xe6b19fe88b8f, 0xe6af95e4b89ae5b9b4e5baa6e5889be4b89ae4b8aae4bd93e7bb8fe890a5efbc8c33e5b9b4e58685e6af8fe688b7e6af8fe5b9b43234303030e58583e99990e9a29de689a3e5878fe7a88ee8b4b9, 0xe6b19fe88b8fe79c81e4babae7a4bee58e85efbc88e694bfe7ad96e6b885e58d95efbc89, 25),
(0x53482d303031, 0xe4b880e6aca1e680a7e6b182e8818ce8a1a5e8b4b4, 0x484f5553494e47, 0x53545544454e542c4752414455415445, 1000.00, 0xe6af8fe4baba31303030e58583, 0xe6af95e4b89ae5b9b4e5baa6e69c89e5b0b1e4b89ae5889be4b89ae6848fe684bfe79a84e59bb0e99abee6af95e4b89ae7949f, 0x68747470733a2f2f7777772e7368616e676861692e676f762e636e2f6e7731323334342f32303235303532322f31303561396335353737653334666665396365326566343765363131343930352e68746d6c, '2025-01-01', NULL, 0xe4bd8ee4bf9d2ce99bb6e5b0b1e4b89a2ce789b9e59bb02ce6ae8be796bee4baba2ce58aa9e5ada6e8b4b7e6acbe2ce6af95e4b89ae7949f, 0xe4b88ae6b5b7, 0xe4b88ae6b5b7e6af95e4b89ae5b9b4e5baa6e59bb0e99abee6af95e4b89ae7949fe58fafe794b3e9a286e4b880e6aca1e680a7e6b182e8818ce8a1a5e8b4b431303030e58583, 0xe4b88ae6b5b7e5b882e4babae6b091e694bfe5ba9c, 26),
(0x53482d303032, 0xe9a696e6aca1e5889be4b89ae4b880e6aca1e680a7e8a1a5e8b4b4efbc8838303030e58583efbc89, 0x454e5452455052454e455552, 0x454e5452455052454e4555522c47524144554154452c53545544454e54, 8000.00, 0xe4b880e6aca1e680a738303030e58583, 0xe9a696e6aca1e5889be4b89aefbc9be58fa6e4baab33e5b9b4e58685e6af8fe5b9b4322e34e4b887e7a88ee8b4b9e5878fe5858d, 0x68747470733a2f2f7777772e7368616e676861692e676f762e636e2f6e7733313430362f32303235303532332f64613730353361626538336434643037613937303364343338303839376636392e68746d6c, '2025-01-01', NULL, 0xe5889be4b89a2ce9a696e6aca1e5889be4b89a2ce6af95e4b89ae7949f2ce59ca8e6a0a1e7949f, 0xe4b88ae6b5b7, 0xe4b88ae6b5b7e6af95e4b89ae7949fe9a696e6aca1e5889be4b89ae4b880e6aca1e680a7e8a1a5e8b4b438303030e58583efbc8ce5889be4b89ae4baab33e5b9b4e6af8fe5b9b4322e34e4b887e7a88ee8b4b9e5878fe5858d, 0xe4b88ae6b5b7e5b882e4babae6b091e694bfe5ba9c, 27),
(0x53482d303033, 0xe781b5e6b4bbe5b0b1e4b89ae7a4bee4bf9de8a1a5e8b4b4efbc88e7a6bbe6a0a132e5b9b4e58685efbc89, 0x484f5553494e47, 0x4752414455415445, 0.00, 0xe68c89e7a4bee4bf9de7bcb4e8b4b9e59fbae695b0e4b88be99990e8aea1e7ae97e8a1a5e8b4b4, 0xe7a6bbe6a0a132e5b9b4e58685e69caae5b0b1e4b89ae6af95e4b89ae7949fe5889de6aca1e781b5e6b4bbe5b0b1e4b89ae5b9b6e58f82e58aa0e59f8ee99587e8818ce5b7a5e585bbe880812fe58cbbe79697e4bf9de999a9, 0x68747470733a2f2f7777772e7368616e676861692e676f762e636e2f6e7731323334342f32303235303532322f31303561396335353737653334666665396365326566343765363131343930352e68746d6c, '2025-01-01', NULL, 0xe6af95e4b89ae7949f2ce781b5e6b4bbe5b0b1e4b89a2ce7a4bee4bf9d, 0xe4b88ae6b5b7, 0xe7a6bbe6a0a132e5b9b4e58685e781b5e6b4bbe5b0b1e4b89ae5b9b6e58f82e4bf9defbc8ce68c89e7bcb4e8b4b9e59fbae695b0e4b88be99990e7bb99e4ba88e7a4bee4bf9de8a1a5e8b4b4, 0xe4b88ae6b5b7e5b882e4babae6b091e694bfe5ba9c, 28),
(0x53482d303034, 0xe5889be4b89ae68b85e4bf9de8b4b7e6acbeefbc88e694bee5aebde794b3e8afb7efbc89, 0x454e5452455052454e455552, 0x454e5452455052454e4555522c47524144554154452c53545544454e54, 300000.00, 0xe8b4a2e694bfe8b4b4e681afefbc88e6a8a1e68b9fe58fa3e5be84efbc89, 0xe694bee5aebde69da1e4bbb6efbc9ae58581e8aeb8e69caae6b885e581bfe58aa9e5ada6e8b4b72fe6b688e8b4b9e8b4b7e68385e586b5e4b88be794b3e8afb7, 0x68747470733a2f2f7777772e676f762e636e2f6c69616e626f2f646966616e672f3230323530352f636f6e74656e745f373032343936392e68746d, '2025-01-01', NULL, 0xe5889be4b89a2ce6af95e4b89ae7949f2ce59ca8e6a0a1e7949f, 0xe4b88ae6b5b7, 0xe4b88ae6b5b7e694bee5aebde5889be4b89ae68b85e4bf9de8b4b7e6acbee69da1e4bbb6efbc8ce58581e8aeb8e69c89e69caae6b885e581bfe8b4b7e6acbee88085e794b3e8afb7efbc8ce9bc93e58ab1e5adb5e58c96e59fbae59cb0e5858de8b4b9e59cbae59cb0, 0xe4b8ade59bbde694bfe5ba9ce7bd91efbc88e4b88ae6b5b7e58aa8e68081efbc89, 29),
(0x424a2d303031, 0xe4b880e6aca1e680a7e6b182e8818ce8a1a5e8b4b4, 0x484f5553494e47, 0x53545544454e542c4752414455415445, 1000.00, 0xe6af8fe4baba31303030e58583, 0xe59ca8e4baace999a2e6a0a1e6af95e4b89ae5ada6e5b9b4e585ade7b1bbe59bb0e99abee6af95e4b89ae7949f, 0x68747470733a2f2f72736a2e6265696a696e672e676f762e636e2f7765696d656e68752f776d74677a2f3230323530332f7432303235303332375f343034363236322e68746d6c, '2025-01-01', NULL, 0xe4bd8ee4bf9d2ce99bb6e5b0b1e4b89a2ce789b9e59bb02ce6ae8be796bee4baba2ce58aa9e5ada6e8b4b7e6acbe2ce6af95e4b89ae7949f, 0xe58c97e4baac, 0xe58c97e4baace6af95e4b89ae5ada6e5b9b4e585ade7b1bbe59bb0e99abee6af95e4b89ae7949fe58fafe794b3e9a286e4b880e6aca1e680a7e6b182e8818ce8a1a5e8b4b431303030e58583, 0xe58c97e4baace5b882e4babae58a9be8b584e6ba90e5928ce7a4bee4bc9ae4bf9de99a9ce5b180, 30),
(0x424a2d303032, 0xe4b880e6aca1e680a7e5889be4b89ae8a1a5e8b4b4efbc8838303030e58583efbc89, 0x454e5452455052454e455552, 0x454e5452455052454e4555522c47524144554154452c53545544454e54, 10000.00, 0x38303030e585832be68b9be794a8e8a1a5e8b4b4efbc8ce680bbe9a29de4b88de8b68531e4b887, 0xe9a696e6aca1e5889be58a9ee5889be4b89ae7bb84e7bb87efbc9be7bcb4e7a4bee4bf9de7b4afe8aea1e6bba136e4b8aae69c88efbc9be6ada3e5b8b8e7bb8fe890a5e7bab3e7a88e31e5b9b4e4bba5e4b88a, 0x68747470733a2f2f74796a7273776a2e6265696a696e672e676f762e636e2f66777a6c2f6a7963792f3230323630372f503032303236303731333239303631313932393236372e706466, '2025-01-01', NULL, 0xe5889be4b89a2ce9a696e6aca1e5889be4b89a2ce7a4bee4bf9d36e4b8aae69c88, 0xe58c97e4baac, 0xe58c97e4baace9a696e6aca1e5889be4b89ae7bb84e7bb87e8a1a5e8b4b438303030e58583efbc8ce6af8fe68b9be794a831e5908de69cace5b882e688b7e7b18de58ab3e58aa8e58a9be5868de8a1a531303030e58583efbc88e680bbe9a29de289a431e4b887efbc89, 0xe58c97e4baace5b882e683a0e4bc81e694bfe7ad96e5afbce888aae68c87e58d97, 31),
(0x424a2d303033, 0xe5889be4b89ae68b85e4bf9de8b4b7e6acbe2be5889be4b89ae59bade5858de8b4b9e59cbae59cb0, 0x454e5452455052454e455552, 0x454e5452455052454e4555522c47524144554154452c53545544454e54, 500000.00, 0xe8b4b7e6acbee8b4b4e681af2be5858de8b4b9e59cbae59cb0efbc88e6a8a1e68b9fe58fa3e5be84efbc89, 0xe9ab98e6a0a1e6af95e4b89ae7949fe5889be4b89ae9a1b9e79baeefbc9be5a4a7e5ada6e7949fe5889be4b89ae59bade5858de8b4b9e69c8de58aa133e5b9b4efbc9be5adb5e58c96e59fbae59cb0333025e59cbae59cb0e5858de8b4b9, 0x68747470733a2f2f7777772e6265696a696e672e676f762e636e2f7a68656e6763652f7a636a642f7a6377642f67786279736a7963792f, '2025-01-01', NULL, 0xe5889be4b89a2ce6af95e4b89ae7949f2ce59ca8e6a0a1e7949f2ce5858de8b4b9e59cbae59cb0, 0xe58c97e4baac, 0xe58c97e4baace5889be4b89ae68b85e4bf9de8b4b7e6acbee8b4b4e681afefbc8ce5889be4b89ae59bade5858de8b4b9e69c8de58aa133e5b9b4efbc8ce5adb5e58c96e59fbae59cb0333025e59cbae59cb0e5858de8b4b9e7bb99e6af95e4b89ae7949f, 0xe58c97e4baace5b882e4babae6b091e694bfe5ba9c, 32),
(0x53432d303031, 0xe5889be4b89ae8a1a5e8b4b4efbc88e58886e6aeb5e69c80e9ab98312e35e4b887efbc89, 0x454e5452455052454e455552, 0x454e5452455052454e4555522c47524144554154452c53545544454e542c5645544552414e2c4641524d4552, 15000.00, 0xe8bf90e890a536e4b8aae69c8835303030e58583efbc8c3138e4b8aae69c88e69c80e9ab9831e4b887efbc8ce98080e5bdb9e5a3abe585b5312e35e4b887, 0xe7a6bbe6a0a135e5b9b4e58685e6af95e4b89ae7949fe7ad89e9a696e6aca1e5889be58a9ee5b08fe5beae2fe4b8aae4bd93efbc9be58886e6aeb5e8a1a5e8b4b4, 0x68747470733a2f2f7777772e73632e676f762e636e2f31303436322f7a66776a74732f323032352f342f33302f38636664363936323863396134343833613631343434393533303731393262642f66696c65732f254535254237253944254535253841253945254538254137253834362545352538462542372545352538352541432545352542432538302545372538392538382e706466, '2025-01-01', NULL, 0xe5889be4b89a2ce6af95e4b89ae7949f2ce59ca8e6a0a1e7949f2ce98080e5bdb9e5869be4baba2ce8bf94e4b9a1, 0xe59b9be5b79d, 0xe59b9be5b79de5889be4b89ae8a1a5e8b4b4e58886e6aeb5e58f91e694beefbc9ae8bf90e890a536e4b8aae69c88e8a1a535303030e58583efbc8c3138e4b8aae69c88e69c80e9ab9831e4b887efbc8ce98080e5bdb9e5a3abe585b5e69c80e9ab98312e35e4b887, 0xe59b9be5b79de79c81e4babae6b091e694bfe5ba9c, 33),
(0x53432d303032, 0xe5889be4b89ae68b85e4bf9de8b4b7e6acbe2be99d92e5b9b4e590afe58aa8e8b584e98791, 0x454e5452455052454e455552, 0x454e5452455052454e4555522c47524144554154452c53545544454e54, 6000000.00, 0xe4b8aae4babae69c80e9ab983530e4b8872fe4bc81e4b89a363030e4b887efbc9be99d92e5b9b4332d3130e4b887e5858de681afe5858de68b85e4bf9defbc88e6a8a1e68b9fe58fa3e5be84efbc89, 0xe59ca8e5b79de5889be4b89ae4b889e5b9b4e5868531382d3430e5b281e99d92e5b9b4efbc9be9ab98e6a0a1e6af95e4b89ae7949fe5889be4b89ae88085, 0x68747470733a2f2f7777772e73632e676f762e636e2f31303436322f31303730352f31303730372f323032352f352f32312f36333962656561356339656434626533616466643532356263393134323761392e7368746d6c, '2025-01-01', NULL, 0xe5889be4b89a2ce6af95e4b89ae7949f2ce59ca8e6a0a1e7949f2ce99d92e5b9b4, 0xe59b9be5b79d, 0xe59b9be5b79de4b8aae4babae5889be4b89ae8b4b7e6acbee69c80e9ab983530e4b8872fe4bc81e4b89a363030e4b887efbc9b31382d3430e5b281e99d92e5b9b433e5b9b4e58685e59ca8e5b79de5889be4b89ae58fafe88eb7332d3130e4b887e5858de681afe5858de68b85e4bf9de590afe58aa8e8b584e98791, 0xe59b9be5b79de79c81e4babae6b091e694bfe5ba9c, 34),
(0x48422d303031, 0xe4b880e6aca1e680a7e5889be4b89ae8a1a5e8b4b4efbc8835303030e58583efbc89, 0x454e5452455052454e455552, 0x454e5452455052454e4555522c47524144554154452c53545544454e54, 5000.00, 0xe4b880e6aca1e680a735303030e58583, 0xe6af95e4b89a35e5b9b4e58685e6af95e4b89ae7949fe59ca8e98482e5889de6aca1e5889be58a9ee5b08fe5beae2fe4b8aae4bd932fe59088e4bd9ce7a4beefbc9be7bb8fe890a536e4b8aae69c88e4bba5e4b88ae5b8a6e58aa8e5b0b1e4b89a32e4baba, 0x68747470733a2f2f7273742e68756265692e676f762e636e2f7a667878676b2f7a632f71747a64676b776a2f3230323630362f7432303236303630325f353934393931312e7368746d6c, '2025-01-01', NULL, 0xe5889be4b89a2ce6af95e4b89ae7949f2ce59ca8e6a0a1e7949f2ce7bb8fe890a536e4b8aae69c88, 0xe6b996e58c97, 0xe6b996e58c97e6af95e4b89a35e5b9b4e58685e5889de6aca1e5889be4b89ae5b9b6e7bb8fe890a536e4b8aae69c88e4bba5e4b88ae38081e5b8a6e58aa8e5b0b1e4b89a32e4babaefbc8ce4b880e6aca1e680a7e8a1a5e8b4b435303030e58583, 0xe6b996e58c97e79c81e4babae58a9be8b584e6ba90e5928ce7a4bee4bc9ae4bf9de99a9ce58e85, 35),
(0x464a2d303031, 0xe4b880e6aca1e680a7e6b182e8818ce8a1a5e8b4b4efbc8832303030e58583efbc89, 0x484f5553494e47, 0x53545544454e542c4752414455415445, 2000.00, 0xe6af8fe4baba32303030e58583, 0xe585ade7b1bbe59bb0e99abee6af95e4b89ae7949fefbc88e590abe58aa9e5ada6e8b4b7e6acbeefbc89, 0x68747470733a2f2f7273742e66756a69616e2e676f762e636e2f7a772f7a667878676b2f7a667878676b6d6c2f7a797977677a2f6a79636a2f3230323531302f503032303235313031363538343738373939323632342e706466, '2025-01-01', NULL, 0xe4bd8ee4bf9d2ce99bb6e5b0b1e4b89a2ce789b9e59bb02ce6ae8be796bee4baba2ce58aa9e5ada6e8b4b7e6acbe2ce6af95e4b89ae7949f, 0xe7a68fe5bbba, 0xe7a68fe5bbbae585ade7b1bbe59bb0e99abee6af95e4b89ae7949fe58fafe794b3e9a286e4b880e6aca1e680a7e6b182e8818ce8a1a5e8b4b432303030e58583, 0xe7a68fe5bbbae79c81e4babae58a9be8b584e6ba90e5928ce7a4bee4bc9ae4bf9de99a9ce58e85, 36),
(0x464a2d303032, 0xe5a4a7e4b8ade4b893e6af95e4b89ae7949fe5889be4b89ae9a1b9e79baee8b584e98791e689b6e68c81efbc88332d3130e4b887efbc89, 0x454e5452455052454e455552, 0x454e5452455052454e4555522c47524144554154452c53545544454e54, 100000.00, 0x33e4b8872d3130e4b887e58583e8b584e98791e689b6e68c81, 0xe5a4a7e4b8ade4b893e6af95e4b89ae7949fe5889be4b89ae9a1b9e79baeefbc9be7bb8fe981b4e98089e79c81e7baa7e8b584e58aa9, 0x68747470733a2f2f7273742e66756a69616e2e676f762e636e2f7a772f7a667878676b2f7a667878676b6d6c2f666c6667676678776a2f676678776a2f3230323530362f7432303235303631385f363932383833342e68746d, '2025-01-01', NULL, 0xe5889be4b89a2ce6af95e4b89ae7949f2ce59ca8e6a0a1e7949f2ce9a1b9e79baee981b4e98089, 0xe7a68fe5bbba, 0xe7a68fe5bbbae981b4e98089e5a4a7e4b8ade4b893e6af95e4b89ae7949fe5889be4b89ae9a1b9e79baeefbc8ce7bb99e4ba8833e4b8872d3130e4b887e58583e8b584e98791e689b6e68c81, 0xe7a68fe5bbbae79c81e4babae58a9be8b584e6ba90e5928ce7a4bee4bc9ae4bf9de99a9ce58e85, 37),
(0x53442d303031, 0xe4bf83e8bf9be99d92e5b9b4e9ab98e8b4a8e9878fe5b0b1e4b89ae88ba5e5b9b2e68eaae696bd, 0x484f5553494e47, 0x53545544454e542c47524144554154452c454e5452455052454e455552, 0.00, 0xe689a9e5b297e8a1a5e58aa92be7a4bee4bf9de8a1a5e8b4b42be5889be4b89ae8a1a5e8b4b4efbc88e6a8a1e68b9fe58fa3e5be84efbc89, 0xe9ab98e6a0a1e6af95e4b89ae7949fe7ad89e99d92e5b9b4e5b0b1e4b89ae5889be4b89ae7b3bbe58897e68eaae696bd, 0x687474703a2f2f7777772e7368616e646f6e672e676f762e636e2f6a706161732d6a706f6c6963792d7765622d7365727665722f66726f6e742f696e666f2f64657461696c3f6969643d3633386239643566383632623461313462346164636432386464333165376637, '2025-01-01', NULL, 0xe6af95e4b89ae7949f2ce59ca8e6a0a1e7949f2ce5889be4b89a2ce5b0b1e4b89a, 0xe5b1b1e4b89c, 0xe5b1b1e4b89c3131e983a8e997a8e58db0e58f91e4bf83e8bf9be99d92e5b9b4e9ab98e8b4a8e9878fe5b0b1e4b89ae68eaae696bdefbc9ae689a9e5b297e8a1a5e58aa9e38081e7a4bee4bf9de8a1a5e8b4b4e38081e5889be4b89ae8a1a5e8b4b4e7ad89, 0xe5b1b1e4b89ce79c81e4babae6b091e694bfe5ba9c, 38),
(0x53442d303032, 0xe99d92e5b29be99d92e5b9b4e4babae6898de5ae89e5aeb6e8b4b92be4bd8fe688bfe8a1a5e8b4b4, 0x484f5553494e47, 0x47524144554154452c454e5452455052454e455552, 150000.00, 0xe4b880e6aca1e680a7e5ae89e5aeb6e8b4b92be4bd8fe688bfe8a1a5e8b4b4efbc88e6a8a1e68b9fe58fa3e5be84efbc89, 0xe99d92e5b9b4e4babae6898de59ca8e99d92e5889be696b0e5889be4b89aefbc9be58fafe58fa0e58aa0e5889be4b89ae8a1a5e8b4b4, 0x687474703a2f2f6d2e746f757469616f2e636f6d2f67726f75702f373639303337383634333231383637383238372f, '2025-01-01', NULL, 0xe6af95e4b89ae7949f2ce5889be4b89a2ce4bd8fe688bf2ce5ae89e5aeb6e8b4b9, 0xe5b1b1e4b89c, 0xe99d92e5b29be99d92e5b9b4e4babae6898de58fafe4baabe4b880e6aca1e680a7e5ae89e5aeb6e8b4b9e38081e4bd8fe688bfe8a1a5e8b4b4e58f8ae5889be4b89ae8a1a5e8b4b4e7ad89, 0xe99d92e5b29be8a5bfe6b5b7e5b2b8e696b0e58cbaefbc88e694bfe58aa1e6b4bbe58aa8efbc89, 39),
(0x484e2d303031, 0xe99d92e5b9b4e5b0b1e4b89ae5889be4b89ae694afe68c81e8aea1e58892, 0x484f5553494e47, 0x53545544454e542c47524144554154452c454e5452455052454e455552, 0.00, 0xe590b8e7bab3e5b0b1e4b89ae8a1a5e8b4b4e6af8fe4babae289a431353030e58583efbc88e6a8a1e68b9fe58fa3e5be84efbc89, 0xe5889de5889b33e5b9b4e58685e4bc81e4b89ae590b8e7bab3e6af95e4b89ae5b9b4e5baa62fe7a6bbe6a0a132e5b9b4e58685e6af95e4b89ae7949fefbc9be7adbee8aea2e59088e5908ce7bcb4e7a4bee4bf9d33e4b8aae69c88, 0x687474703a2f2f68756e616e2e676f762e636e2f686e737a662f686e79772f7a7764742f3230323530332f7432303235303332325f33333631393536312e68746d6c, '2025-01-01', NULL, 0xe6af95e4b89ae7949f2ce59ca8e6a0a1e7949f2ce5889be4b89a2ce590b8e7bab3e5b0b1e4b89a, 0xe6b996e58d97, 0xe6b996e58d9732303235e99d92e5b9b4e5b0b1e4b89ae5889be4b89ae694afe68c81e8aea1e58892efbc9ae5889de5889be4bc81e4b89ae590b8e7bab3e99d92e5b9b4e5b0b1e4b89ae6af8fe4babae69c80e9ab9831353030e58583e8a1a5e8b4b4, 0xe6b996e58d97e79c81e4babae6b091e694bfe5ba9c, 40),
(0x48412d303031, 0xe4b880e6aca1e680a7e6b182e8818ce8a1a5e8b4b4efbc8832303030e58583efbc89, 0x484f5553494e47, 0x53545544454e542c4752414455415445, 2000.00, 0xe6af8fe4baba32303030e58583, 0xe6af95e4b89ae5ada6e5b9b4e7a7afe69e81e6b182e8818ce79a84e59bb0e99abee6af95e4b89ae7949fefbc88e4bd8ee4bf9d2fe6ae8be796be2fe58aa9e5ada6e8b4b7e6acbee7ad89efbc89, 0x68747470733a2f2f687273732e68656e616e2e676f762e636e2f323032352f30392d30352f333230393139332e68746d6c, '2025-01-01', NULL, 0xe4bd8ee4bf9d2ce6ae8be796bee4baba2ce58aa9e5ada6e8b4b7e6acbe2ce6af95e4b89ae7949f, 0xe6b2b3e58d97, 0xe6b2b3e58d97e6af95e4b89ae5ada6e5b9b4e59bb0e99abee6af95e4b89ae7949fe58fafe794b3e9a286e4b880e6aca1e680a7e6b182e8818ce8a1a5e8b4b432303030e58583, 0xe6b2b3e58d97e79c81e4babae58a9be8b584e6ba90e5928ce7a4bee4bc9ae4bf9de99a9ce58e85, 41),
(0x48412d303032, 0xe5889be4b89ae68b85e4bf9de8b4b7e6acbe2be4b880e6aca1e680a7e5889be4b89ae8a1a5e8b4b4, 0x454e5452455052454e455552, 0x454e5452455052454e4555522c47524144554154452c53545544454e54, 300000.00, 0xe4b8aae4babae69c80e9ab983330e4b8872fe59088e4bc99343030e4b887efbc9be5889be4b89ae8a1a5e8b4b435303030e58583, 0xe7a6bbe6a0a132e5b9b4e58685e6af95e4b89ae7949fe9a696e6aca1e5889be58a9ee5b08fe5beae2fe4b8aae4bd93, 0x68747470733a2f2f7777772e68656e616e2e676f762e636e2f323032352f30372d31382f333138313936352e68746d6c, '2025-01-01', NULL, 0xe5889be4b89a2ce6af95e4b89ae7949f2ce59ca8e6a0a1e7949f2ce9a696e6aca1e5889be4b89a, 0xe6b2b3e58d97, 0xe6b2b3e58d97e5889be4b89ae68b85e4bf9de8b4b7e6acbee4b8aae4babae69c80e9ab983330e4b887efbc88e59088e4bc99343030e4b887efbc89efbc8ce7a6bbe6a0a132e5b9b4e58685e9a696e6aca1e5889be4b89ae58fa6e4baab35303030e58583e8a1a5e8b4b4, 0xe6b2b3e58d97e79c81e4babae6b091e694bfe5ba9c, 42),
(0x41482d303031, 0xe4b880e6aca1e680a7e6b182e8818ce8a1a5e8b4b4efbc8831353030e58583efbc89, 0x484f5553494e47, 0x53545544454e542c4752414455415445, 1500.00, 0xe6af8fe4baba31353030e58583, 0xe6af95e4b89ae5ada6e5b9b4e59bb0e99abee6af95e4b89ae7949fefbc88e4bd8ee4bf9d2fe99bb6e5b0b1e4b89a2fe58aa9e5ada6e8b4b7e6acbee7ad89efbc89, 0x68747470733a2f2f687273732e61682e676f762e636e2f7a787a782f677367672f38303737353332382e68746d6c, '2025-01-01', NULL, 0xe4bd8ee4bf9d2ce99bb6e5b0b1e4b89a2ce789b9e59bb02ce6ae8be796bee4baba2ce58aa9e5ada6e8b4b7e6acbe2ce6af95e4b89ae7949f, 0xe5ae89e5bebd, 0xe5ae89e5bebde6af95e4b89ae5ada6e5b9b4e59bb0e99abee6af95e4b89ae7949fe58fafe794b3e9a286e4b880e6aca1e680a7e6b182e8818ce8a1a5e8b4b431353030e58583, 0xe5ae89e5bebde79c81e4babae58a9be8b584e6ba90e5928ce7a4bee4bc9ae4bf9de99a9ce58e85, 43),
(0x41482d303032, 0xe4b880e6aca1e680a7e5889be4b89ae8a1a5e8b4b4efbc8831e4b887e58583efbc89, 0x454e5452455052454e455552, 0x454e5452455052454e4555522c47524144554154452c53545544454e54, 10000.00, 0xe4b880e6aca1e680a731e4b887e58583, 0xe7a6bbe6a0a132e5b9b4e58685e6af95e4b89ae7949fe9a696e6aca1e5889be58a9ee5b08fe5beaee4bc81e4b89aefbc9be8bf90e890a533e4b8aae69c88e4bba5e4b88ae5b9b6e7bcb4e7a4bee4bf9d, 0x68747470733a2f2f687273732e61682e676f762e636e2f7a787a782f7a747a6c2f61717371647a63797a6272676a7a7878642f38303738383336322e68746d6c, '2025-01-01', NULL, 0xe5889be4b89a2ce6af95e4b89ae7949f2ce59ca8e6a0a1e7949f2ce8bf90e890a533e4b8aae69c88, 0xe5ae89e5bebd, 0xe5ae89e5bebde7a6bbe6a0a132e5b9b4e58685e6af95e4b89ae7949fe9a696e6aca1e5889be58a9ee5b08fe5beaee4bc81e4b89ae38081e8bf90e890a533e4b8aae69c88e5b9b6e7bcb4e7a4bee4bf9defbc8ce4b880e6aca1e680a7e8a1a5e8b4b431e4b887e58583, 0xe5ae89e5bebde79c81e4babae58a9be8b584e6ba90e5928ce7a4bee4bc9ae4bf9de99a9ce58e85, 44),
(0x41482d303033, 0xe5889be4b89ae68b85e4bf9de8b4b7e6acbeefbc88e4b8aae4babae69c80e9ab983530e4b887efbc89, 0x454e5452455052454e455552, 0x454e5452455052454e4555522c47524144554154452c53545544454e54, 500000.00, 0xe8b4a2e694bfe8b4b4e681afefbc88e6a8a1e68b9fe58fa3e5be84efbc89, 0xe7aca6e59088e69da1e4bbb6e79a84e9ab98e6a0a1e6af95e4b89ae7949fe5889be4b89ae88085, 0x68747470733a2f2f6d31323333332e636e2f706f6c6963792f73706365662e68746d6c, '2025-01-01', NULL, 0xe5889be4b89a2ce6af95e4b89ae7949f2ce59ca8e6a0a1e7949f, 0xe5ae89e5bebd, 0xe5ae89e5bebde9ab98e6a0a1e6af95e4b89ae7949fe5889be4b89ae58fafe794b3e8afb7e69c80e9ab983530e4b887e58583e5889be4b89ae68b85e4bf9de8b4b7e6acbeefbc8ce69c9fe99990e4b88de8b68533e5b9b4, 0xe5ae89e5bebde79c81e998b3e58589e5b0b1e4b89ae7bd91e4b88ae69c8de58aa1e5a4a7e58e85, 45),
(0x43512d303031, 0xe4b880e6aca1e680a7e5889be4b89ae8a1a5e8b4b4efbc88e69c80e9ab9838303030e58583efbc89, 0x454e5452455052454e455552, 0x454e5452455052454e4555522c47524144554154452c53545544454e54, 8000.00, 0xe4b880e6aca1e680a7e69c80e9ab9838303030e58583, 0xe9ab98e6a0a1e6af95e4b89ae7949fe9a696e6aca1e5889be58a9ee5b08fe5beaee4bc81e4b89a2fe4b8aae4bd93e5b7a5e59586e688b7, 0x687474703a2f2f7777772e63712e676f762e636e2f7a77676b2f7a667878676b7a6c2f66647a64676b6e722f7a646d7378782f636a6a792f636a6a795f7373716b2f3230323531322f7432303235313232305f31353235393130362e68746d6c, '2025-01-01', NULL, 0xe5889be4b89a2ce6af95e4b89ae7949f2ce59ca8e6a0a1e7949f2ce9a696e6aca1e5889be4b89a, 0xe9878de5ba86, 0xe9878de5ba86e9ab98e6a0a1e6af95e4b89ae7949fe9a696e6aca1e5889be58a9ee5b08fe5beaee4bc81e4b89aefbc8ce4b880e6aca1e680a7e5889be4b89ae8a1a5e8b4b4e69c80e9ab9838303030e58583, 0xe9878de5ba86e5b882e4babae6b091e694bfe5ba9c, 46),
(0x43512d303032, 0xe5889be4b89ae68b85e4bf9de8b4b7e6acbe2be5858de8b4b9e5889be4b89ae5b7a5e4bd8d, 0x454e5452455052454e455552, 0x454e5452455052454e4555522c47524144554154452c53545544454e54, 500000.00, 0xe4b8aae4babae69c80e9ab983530e4b8872fe4bc81e4b89a363030e4b887efbc88e6a8a1e68b9fe58fa3e5be84efbc89, 0xe9ab98e6a0a1e6af95e4b89ae7949fe5889be4b89ae88085efbc9be6af8fe5b9b4e68f90e4be9b31e4b887e4b8aae5858de8b4b9e5889be4b89ae5b7a5e4bd8d, 0x687474703a2f2f7777772e63712e676f762e636e2f797764742f6a7263712f3230323530372f7432303235303732315f31343833303732352e68746d6c, '2025-01-01', NULL, 0xe5889be4b89a2ce6af95e4b89ae7949f2ce59ca8e6a0a1e7949f2ce5858de8b4b9e59cbae59cb0, 0xe9878de5ba86, 0xe9878de5ba86e5889be4b89ae68b85e4bf9de8b4b7e6acbee4b8aae4babae69c80e9ab983530e4b8872fe4bc81e4b89a363030e4b887efbc8ce6af8fe5b9b4e68f90e4be9b31e4b887e4b8aae5858de8b4b9e5889be4b89ae5b7a5e4bd8d, 0xe9878de5ba86e5b882e4babae6b091e694bfe5ba9c, 47),
(0x43512d303033, 0xe8818ce4b89ae68a80e883bde59fb9e8aeade8a1a5e8b4b42be781b5e6b4bbe5b0b1e4b89ae7a4bee4bf9de8a1a5e8b4b4, 0x484f5553494e47, 0x53545544454e542c4752414455415445, 6000.00, 0xe59fb9e8aeade8a1a5e8b4b4e69c80e9ab9836303030e58583efbc9be7a4bee4bf9de8a1a5e8b4b4e289a4e7bcb4e8b4b9322f33e69c80e995bf32e5b9b4, 0xe9ab98e6a0a1e6af95e4b89ae7949fe58f82e58aa0e8818ce4b89ae68a80e883bde59fb9e8aeadefbc9be7a6bbe6a0a132e5b9b4e58685e781b5e6b4bbe5b0b1e4b89ae5b9b6e58f82e4bf9d, 0x68747470733a2f2f63712e676f762e636e2f7a77676b2f7a667878676b7a6c2f66647a64676b6e722f7a646d7378782f636a6a792f636a6a795f7373716b2f3230323531302f7432303235313031305f31353036383435342e68746d6c, '2025-01-01', NULL, 0xe6af95e4b89ae7949f2ce59ca8e6a0a1e7949f2ce781b5e6b4bbe5b0b1e4b89a2ce7a4bee4bf9d2ce59fb9e8aead, 0xe9878de5ba86, 0xe9878de5ba86e68a80e883bde59fb9e8aeade8a1a5e8b4b4e69c80e9ab9836303030e58583efbc9be7a6bbe6a0a132e5b9b4e58685e781b5e6b4bbe5b0b1e4b89ae7a4bee4bf9de8a1a5e8b4b4e289a4e7bcb4e8b4b9322f33e69c80e995bf32e5b9b4, 0xe9878de5ba86e5b882e4babae6b091e694bfe5ba9c, 48),
(0x47582d303031, 0xe5b0b1e4b89ae8a781e4b9a0e8a1a5e8b4b4efbc88e6af8fe69c8831353030e58583efbc89, 0x484f5553494e47, 0x53545544454e542c4752414455415445, 2000.00, 0xe6af8fe4babae6af8fe69c8831353030e58583efbc8ce79599e794a8e78e87353025e4bba5e4b88ae68f90e887b332303030e58583, 0xe7a6bbe6a0a132e5b9b4e58685e69caae5b0b1e4b89ae9ab98e6a0a1e6af95e4b89ae7949fe58f82e58aa0e8a781e4b9a0e59fbae59cb0e8a781e4b9a0, 0x687474703a2f2f7273742e67787a662e676f762e636e2f7a77676b2f7878676b7a6366672f666766786c6d2f7432353932353736302e7368746d6c, '2025-01-01', NULL, 0xe6af95e4b89ae7949f2ce59ca8e6a0a1e7949f2ce8a781e4b9a02ce5b0b1e4b89a, 0xe5b9bfe8a5bf, 0xe5b9bfe8a5bfe8a781e4b9a0e59fbae59cb0e590b8e7bab3e7a6bbe6a0a132e5b9b4e58685e6af95e4b89ae7949fefbc8ce6af8fe4babae6af8fe69c8831353030e58583e8a781e4b9a0e8a1a5e8b4b4efbc8ce79599e794a8e78e87e9ab98e68f90e887b332303030e58583, 0xe5b9bfe8a5bfe5a3aee6978fe887aae6b2bbe58cbae4babae58a9be8b584e6ba90e5928ce7a4bee4bc9ae4bf9de99a9ce58e85, 49),
(0x53582d303031, 0xe5a4b1e4b89ae4bf9de999a9e7a8b3e5b297e8bf94e8bf98efbc8832303235e5b9b4e5baa6efbc89, 0x454e5452455052454e455552, 0x454e5452455052454e4555522c4f54484552, 0.00, 0xe5a4a7e59e8b3530252fe4b8ade5b08fe5beae393025e8bf94e8bf98efbc88e6a8a1e68b9fe58fa3e5be84efbc89, 0xe58f82e4bf9de4bc81e4b89ae7a8b3e5b297efbc9be694bfe7ad96e689a7e8a18ce887b332303235e5b9b43132e69c88e5ba95, 0x687474703a2f2f7777772e736861616e78692e676f762e636e2f7a667878676b2f66647a64676b6e722f7a63776a2f6e737a66626774776a2f737a62662f3230323530392f7432303235303932325f333536393637395f7761702e68746d6c, '2025-01-01', '2025-12-31', 0xe5889be4b89a2ce7a8b3e5b2972ce4bc81e4b89a, 0xe99995e8a5bf, 0xe99995e8a5bfe58f82e4bf9de4bc81e4b89ae5a4b1e4b89ae4bf9de999a9e7a8b3e5b297e8bf94e8bf98efbc9ae5a4a7e59e8b353025e38081e4b8ade5b08fe5beae393025efbc88e7a4bae4be8befbc9ae5b7b2e8bf87e69c9fe694bfe7ad96e887aae58aa8e8bf87e6bba4efbc89, 0xe99995e8a5bfe79c81e4babae6b091e694bfe5ba9c, 50);

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
