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
-- 表数量校验：共 25 张表
-- sys_user, sys_role, sys_user_role, sys_login_log, sys_message (5)
-- biz_landlord, biz_house, biz_rental_contract, biz_guarantee_application, biz_guarantee, biz_guarantee_claim (6)
-- biz_merchant, biz_loan_application, biz_credit_limit, biz_entrust_payment (4)
-- biz_bookkeeping_record, biz_cashflow_report (2)
-- biz_budget_category, biz_budget_setting, biz_transaction, biz_saving_goal (4)
-- biz_anti_fraud_content, biz_fraud_detection_log, biz_credit_report, biz_risk_warning (4)
-- 总计：5 + 6 + 4 + 2 + 4 + 4 = 25 ✓
-- =============================================================
