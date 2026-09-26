-- =============================================================
-- 青启e城 演示系统 数据库结构（25 张表）
-- 对应《青启e城项目系统设计说明书》第 6 章 6.3 数据表结构设计
-- =============================================================
CREATE DATABASE IF NOT EXISTS qingqi DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE qingqi;

-- 6.3.1 青年用户表
CREATE TABLE IF NOT EXISTS `youth_user` (
  `user_id`       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID（工行统一客户ID）',
  `name`          VARCHAR(64)  NOT NULL COMMENT '姓名',
  `id_type`       VARCHAR(16)  NOT NULL DEFAULT '身份证' COMMENT '证件类型',
  `id_number`     VARCHAR(32)  NOT NULL COMMENT '证件号码（加密/脱敏存储）',
  `phone`         VARCHAR(20)  NOT NULL COMMENT '手机号',
  `user_type`     VARCHAR(16)  NOT NULL COMMENT '人群类型（在校大学生/应届毕业生/Z世代青年）',
  `school`        VARCHAR(128) NULL COMMENT '所在院校（在校生填写）',
  `edu_level`     VARCHAR(16)  NULL COMMENT '学历层次',
  `grad_year`     VARCHAR(8)   NULL COMMENT '毕业年份',
  `password_hash` VARCHAR(128) NOT NULL COMMENT '登录密码散列（演示用）',
  `status`        TINYINT      NOT NULL DEFAULT 0 COMMENT '账户状态（0正常/1冻结）',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='青年用户表';

-- 6.3.2 用户授权表
CREATE TABLE IF NOT EXISTS `user_authorization` (
  `auth_id`     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '授权ID',
  `user_id`     BIGINT       NOT NULL COMMENT '用户ID',
  `auth_type`   VARCHAR(32)  NOT NULL COMMENT '授权类型（第三方流水/征信软查询/数据建模）',
  `auth_scope`  VARCHAR(255) NOT NULL COMMENT '授权范围',
  `auth_status` TINYINT      NOT NULL DEFAULT 0 COMMENT '授权状态（0有效/1已解绑）',
  `auth_start`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '授权开始时间',
  `auth_end`    DATETIME     NULL COMMENT '授权结束时间',
  `unbind_time` DATETIME     NULL COMMENT '解绑时间',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`auth_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户授权表';

-- 6.3.3 账户信息表
CREATE TABLE IF NOT EXISTS `account_info` (
  `account_id`     BIGINT      NOT NULL AUTO_INCREMENT COMMENT '账户ID',
  `user_id`        BIGINT      NOT NULL COMMENT '用户ID',
  `account_type`   VARCHAR(16) NOT NULL COMMENT '账户类型（储蓄卡/收款码/信用卡）',
  `account_no`     VARCHAR(32) NOT NULL COMMENT '账号（加密存储）',
  `account_status` TINYINT     NOT NULL DEFAULT 0 COMMENT '账户状态（0正常/1注销）',
  `bind_time`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
  `update_time`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`account_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账户信息表';

-- 6.3.4 租赁合同表
CREATE TABLE IF NOT EXISTS `lease_contract` (
  `contract_id`        BIGINT        NOT NULL AUTO_INCREMENT COMMENT '合同ID',
  `user_id`            BIGINT        NOT NULL COMMENT '租客用户ID',
  `landlord_name`      VARCHAR(64)   NOT NULL COMMENT '房东姓名',
  `landlord_phone`     VARCHAR(20)   NOT NULL COMMENT '房东联系电话',
  `landlord_id_no`     VARCHAR(32)   NOT NULL COMMENT '房东证件号（加密/脱敏存储）',
  `house_address`      VARCHAR(255)  NOT NULL COMMENT '房源地址',
  `monthly_rent`       DECIMAL(12,2) NOT NULL COMMENT '月租金',
  `deposit_amount`     DECIMAL(12,2) NOT NULL COMMENT '押金金额',
  `lease_start`        DATE          NOT NULL COMMENT '租期开始日期',
  `lease_end`          DATE          NOT NULL COMMENT '租期结束日期',
  `contract_file_url`  VARCHAR(500)  NULL COMMENT '租赁合同文件地址',
  `handover_photo_url` VARCHAR(500)  NULL COMMENT '房屋交接照片地址',
  `review_status`      TINYINT       NOT NULL DEFAULT 0 COMMENT '复审状态（0待审/1自动通过/2转人工）',
  `review_result`      VARCHAR(255)  NULL COMMENT 'AI复审意见',
  `review_time`        DATETIME      NULL COMMENT '复审时间',
  `create_time`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`contract_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租赁合同表';

-- 6.3.5 保函申请表
CREATE TABLE IF NOT EXISTS `guarantee_apply` (
  `apply_id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '保函申请ID',
  `contract_id`          BIGINT        NOT NULL COMMENT '租赁合同ID',
  `user_id`              BIGINT        NOT NULL COMMENT '申请人用户ID',
  `guarantee_amount`     DECIMAL(12,2) NOT NULL COMMENT '保函金额',
  `guarantee_term_months` INT          NOT NULL COMMENT '保函期限（月）',
  `fee_rate`             DECIMAL(5,4)  NOT NULL COMMENT '保函费率（年化）',
  `fee_amount`           DECIMAL(12,2) NOT NULL COMMENT '保函费',
  `apply_status`         TINYINT       NOT NULL DEFAULT 0 COMMENT '申请状态（0待房东确认/1待缴费/2已开函/3已失效/4赔付中）',
  `pay_status`           TINYINT       NOT NULL DEFAULT 0 COMMENT '缴费状态（0未缴/1已缴）',
  `create_time`          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  PRIMARY KEY (`apply_id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_contract` (`contract_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='保函申请表';

-- 6.3.6 保函信息表
CREATE TABLE IF NOT EXISTS `guarantee_info` (
  `guarantee_id`     BIGINT        NOT NULL AUTO_INCREMENT COMMENT '保函ID',
  `apply_id`         BIGINT        NOT NULL COMMENT '保函申请ID',
  `guarantee_no`     VARCHAR(32)   NOT NULL COMMENT '保函编号',
  `applicant_id`     BIGINT        NOT NULL COMMENT '申请人（租客）用户ID',
  `beneficiary`      VARCHAR(64)   NOT NULL COMMENT '受益人（房东）',
  `guarantee_amount` DECIMAL(12,2) NOT NULL COMMENT '保函金额',
  `issue_date`       DATE          NOT NULL COMMENT '开立日期',
  `expire_date`      DATE          NOT NULL COMMENT '失效日期',
  `guarantee_status` TINYINT       NOT NULL DEFAULT 0 COMMENT '状态（0有效/1已失效/2赔付中/3已赔付）',
  `e_guarantee_url`  VARCHAR(500)  NULL COMMENT '电子保函地址',
  PRIMARY KEY (`guarantee_id`),
  UNIQUE KEY `uk_no` (`guarantee_no`),
  KEY `idx_apply` (`apply_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='保函信息表';

-- 6.3.7 违约索赔表
CREATE TABLE IF NOT EXISTS `claim_info` (
  `claim_id`        BIGINT        NOT NULL AUTO_INCREMENT COMMENT '索赔ID',
  `guarantee_id`    BIGINT        NOT NULL COMMENT '保函ID',
  `claimant`        VARCHAR(64)   NOT NULL COMMENT '索赔方（房东）',
  `claim_amount`    DECIMAL(12,2) NOT NULL COMMENT '索赔金额',
  `claim_reason`    VARCHAR(255)  NOT NULL COMMENT '索赔事由',
  `review_stage`    TINYINT       NOT NULL DEFAULT 0 COMMENT '裁决阶段（0待初审/1待复核/2已裁决）',
  `ai_review_result` VARCHAR(255) NULL COMMENT 'AI初审结果',
  `final_result`    VARCHAR(255)  NULL COMMENT '最终认定结果',
  `pay_amount`      DECIMAL(12,2) NULL COMMENT '赔付金额',
  `pay_status`      TINYINT       NOT NULL DEFAULT 0 COMMENT '赔付状态（0未赔付/1已赔付）',
  `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '索赔时间',
  PRIMARY KEY (`claim_id`),
  KEY `idx_guarantee` (`guarantee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='违约索赔表';

-- 6.3.8 索赔证据表
CREATE TABLE IF NOT EXISTS `claim_evidence` (
  `evidence_id`   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '证据ID',
  `claim_id`      BIGINT       NOT NULL COMMENT '索赔ID',
  `evidence_type` VARCHAR(32)  NOT NULL COMMENT '证据类型（照片/视频/记录/物品清单）',
  `file_url`      VARCHAR(500) NOT NULL COMMENT '证据文件地址',
  `submitter`     VARCHAR(64)  NOT NULL COMMENT '上传方（房东/租客）',
  `upload_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`evidence_id`),
  KEY `idx_claim` (`claim_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='索赔证据表';

-- 6.3.9 贷款申请表
CREATE TABLE IF NOT EXISTS `loan_apply` (
  `loan_apply_id`   BIGINT        NOT NULL AUTO_INCREMENT COMMENT '贷款申请ID',
  `user_id`         BIGINT        NOT NULL COMMENT '用户ID',
  `loan_type`       VARCHAR(8)    NOT NULL COMMENT '贷款类型（A/B）',
  `apply_amount`    DECIMAL(12,2) NOT NULL COMMENT '申请金额',
  `loan_purpose`    VARCHAR(255)  NOT NULL COMMENT '贷款用途（限合法经营用途）',
  `biz_plan_url`    VARCHAR(500)  NULL COMMENT '创业计划书地址',
  `precheck_status` TINYINT       NOT NULL DEFAULT 0 COMMENT '预审状态（0未预审/1通过/2不通过）',
  `precheck_range`  VARCHAR(64)   NULL COMMENT '预审额度区间',
  `approve_status`  TINYINT       NOT NULL DEFAULT 0 COMMENT '审批状态（0待审批/1通过/2拒绝）',
  `approve_amount`  DECIMAL(12,2) NULL COMMENT '审批额度',
  `interest_rate`   DECIMAL(6,4)  NULL COMMENT '利率',
  `approver`        VARCHAR(64)   NULL COMMENT '审批人',
  `approve_time`    DATETIME      NULL COMMENT '审批时间',
  `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  PRIMARY KEY (`loan_apply_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='贷款申请表';

-- 6.3.10 授信额度表
CREATE TABLE IF NOT EXISTS `credit_limit` (
  `credit_id`     BIGINT        NOT NULL AUTO_INCREMENT COMMENT '授信ID',
  `user_id`       BIGINT        NOT NULL COMMENT '用户ID',
  `credit_type`   VARCHAR(16)   NOT NULL COMMENT '授信类型（A循环额度/B定向额度）',
  `credit_amount` DECIMAL(12,2) NOT NULL COMMENT '授信额度',
  `used_amount`   DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '已用额度',
  `remain_amount` DECIMAL(12,2) NOT NULL COMMENT '剩余额度',
  `valid_start`   DATE          NOT NULL COMMENT '额度生效日期',
  `valid_end`     DATE          NOT NULL COMMENT '额度失效日期',
  `credit_status` TINYINT       NOT NULL DEFAULT 0 COMMENT '额度状态（0有效/1冻结/2已失效）',
  `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`credit_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='授信额度表';

-- 6.3.11 受托支付记录表
CREATE TABLE IF NOT EXISTS `entrusted_payment` (
  `payment_id`    BIGINT        NOT NULL AUTO_INCREMENT COMMENT '支付ID',
  `loan_apply_id` BIGINT        NOT NULL COMMENT '贷款申请ID',
  `merchant_id`   BIGINT        NOT NULL COMMENT '收款商户白名单ID',
  `merchant_name` VARCHAR(128)  NOT NULL COMMENT '收款商户名称',
  `pay_amount`    DECIMAL(12,2) NOT NULL COMMENT '支付金额',
  `pay_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '支付时间',
  `pay_status`    TINYINT       NOT NULL DEFAULT 0 COMMENT '支付状态（0处理中/1成功/2失败）',
  `voucher_url`   VARCHAR(500)  NULL COMMENT '支付凭证地址',
  PRIMARY KEY (`payment_id`),
  KEY `idx_loan` (`loan_apply_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='受托支付记录表';

-- 6.3.12 经营流水表
CREATE TABLE IF NOT EXISTS `biz_flow` (
  `flow_id`        BIGINT        NOT NULL AUTO_INCREMENT COMMENT '流水ID',
  `user_id`        BIGINT        NOT NULL COMMENT '用户ID',
  `source_type`    VARCHAR(16)   NOT NULL COMMENT '数据来源（工行收款码/工行卡/第三方平台）',
  `flow_amount`    DECIMAL(12,2) NOT NULL COMMENT '流水金额',
  `flow_time`      DATETIME      NOT NULL COMMENT '交易时间',
  `counterparty`   VARCHAR(128)  NULL COMMENT '交易对手',
  `flow_type`      VARCHAR(32)   NULL COMMENT '交易类型',
  `auth_id`        BIGINT        NOT NULL COMMENT '授权ID',
  `collect_status` TINYINT       NOT NULL DEFAULT 0 COMMENT '归集状态（0待归集/1已归集）',
  PRIMARY KEY (`flow_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='经营流水表';

-- 6.3.13 交易记录表（transaction 为 MySQL 非保留字，加反引号避免歧义）
CREATE TABLE IF NOT EXISTS `transaction` (
  `trans_id`        BIGINT        NOT NULL AUTO_INCREMENT COMMENT '交易ID',
  `user_id`         BIGINT        NOT NULL COMMENT '用户ID',
  `account_no`      VARCHAR(32)   NOT NULL COMMENT '交易账户（加密存储）',
  `trans_amount`    DECIMAL(12,2) NOT NULL COMMENT '交易金额',
  `trans_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '交易时间',
  `merchant_name`   VARCHAR(128)  NULL COMMENT '商户名称',
  `mcc_code`        VARCHAR(8)    NULL COMMENT '商户类别码（MCC）',
  `category`        VARCHAR(16)   NULL COMMENT '消费分类（餐饮/娱乐/购物/交通/其他）',
  `classify_mode`   TINYINT       NOT NULL DEFAULT 0 COMMENT '归类方式（0自动/1AI建议待确认/2已确认）',
  `classify_status` TINYINT       NOT NULL DEFAULT 0 COMMENT '归类状态（0未归类/1已归类）',
  `trans_direction` TINYINT       NOT NULL DEFAULT 0 COMMENT '交易方向（0支出/1收入）',
  PRIMARY KEY (`trans_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易记录表';

-- 6.3.14 消费预算表
CREATE TABLE IF NOT EXISTS `budget` (
  `budget_id`       BIGINT        NOT NULL AUTO_INCREMENT COMMENT '预算ID',
  `user_id`         BIGINT        NOT NULL COMMENT '用户ID',
  `budget_month`    VARCHAR(7)    NOT NULL COMMENT '预算月份（YYYY-MM）',
  `category`        VARCHAR(16)   NOT NULL COMMENT '消费分类',
  `budget_amount`   DECIMAL(12,2) NOT NULL COMMENT '预算金额',
  `used_amount`     DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '已用金额',
  `remain_ratio`    DECIMAL(5,2)  NULL COMMENT '剩余比例（%）',
  `remind_level`    TINYINT       NOT NULL DEFAULT 0 COMMENT '提醒等级（0正常/1温和/2紧张/3超支）',
  `carryover_amount` DECIMAL(12,2) NULL COMMENT '结余结转金额',
  `status`          TINYINT       NOT NULL DEFAULT 0 COMMENT '预算状态（0进行中/1已结束）',
  PRIMARY KEY (`budget_id`),
  KEY `idx_user_month` (`user_id`, `budget_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消费预算表';

-- 6.3.15 心愿储蓄表
CREATE TABLE IF NOT EXISTS `goal_saving` (
  `saving_id`      BIGINT        NOT NULL AUTO_INCREMENT COMMENT '储蓄ID',
  `user_id`        BIGINT        NOT NULL COMMENT '用户ID',
  `goal_name`      VARCHAR(64)   NOT NULL COMMENT '储蓄目标名称',
  `goal_amount`    DECIMAL(12,2) NULL COMMENT '目标金额',
  `current_amount` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '当前金额',
  `source_type`    TINYINT       NOT NULL DEFAULT 0 COMMENT '资金来源（0一键改存/1预算结余/2手动）',
  `rate_grade`     VARCHAR(16)   NULL COMMENT '利率档位',
  `open_date`      DATE          NOT NULL COMMENT '开户日期',
  `status`         TINYINT       NOT NULL DEFAULT 0 COMMENT '状态（0进行中/1已达成/2已终止）',
  PRIMARY KEY (`saving_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='心愿储蓄表';

-- 6.3.16 理财持仓表
CREATE TABLE IF NOT EXISTS `wealth_holding` (
  `holding_id`     BIGINT        NOT NULL AUTO_INCREMENT COMMENT '持仓ID',
  `user_id`        BIGINT        NOT NULL COMMENT '用户ID',
  `product_type`   VARCHAR(32)   NOT NULL COMMENT '产品类型（货币基金/现金管理理财/短债基金/固收理财/基金定投/积存金）',
  `product_code`   VARCHAR(32)   NOT NULL COMMENT '产品代码',
  `product_name`   VARCHAR(128)  NOT NULL COMMENT '产品名称',
  `holding_amount` DECIMAL(12,2) NOT NULL COMMENT '持有金额',
  `risk_level`     VARCHAR(8)    NOT NULL COMMENT '风险等级（低）',
  `buy_date`       DATE          NOT NULL COMMENT '购买日期',
  `status`         TINYINT       NOT NULL DEFAULT 0 COMMENT '状态（0持有中/1已赎回）',
  PRIMARY KEY (`holding_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='理财持仓表';

-- 6.3.17 征信监测记录表
CREATE TABLE IF NOT EXISTS `credit_monitor` (
  `monitor_id`     BIGINT        NOT NULL AUTO_INCREMENT COMMENT '监测ID',
  `user_id`        BIGINT        NOT NULL COMMENT '用户ID',
  `query_type`     VARCHAR(16)   NOT NULL DEFAULT '软查询' COMMENT '查询类型（软查询）',
  `query_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '查询时间',
  `credit_summary` VARCHAR(500)  NULL COMMENT '征信状态摘要',
  `abnormal_type`  VARCHAR(64)   NULL COMMENT '异常类型（异常借贷/逾期风险）',
  `alert_flag`     TINYINT       NOT NULL DEFAULT 0 COMMENT '预警标志（0无/1有）',
  `auth_id`        BIGINT        NOT NULL COMMENT '授权ID',
  PRIMARY KEY (`monitor_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='征信监测记录表';

-- 6.3.18 风险预警表
CREATE TABLE IF NOT EXISTS `risk_alert` (
  `alert_id`       BIGINT        NOT NULL AUTO_INCREMENT COMMENT '预警ID',
  `user_id`        BIGINT        NOT NULL COMMENT '用户ID',
  `alert_type`     VARCHAR(32)   NOT NULL COMMENT '预警类型（高频借贷/以贷养贷/现金流紧张/预算超支/逾期风险/征信异常/刷单特征）',
  `alert_level`    TINYINT       NOT NULL DEFAULT 1 COMMENT '预警等级',
  `alert_content`  VARCHAR(500)  NOT NULL COMMENT '预警内容',
  `trigger_detail` VARCHAR(500)  NULL COMMENT '触发明细',
  `handle_status`  TINYINT       NOT NULL DEFAULT 0 COMMENT '处理状态（0待处理/1已处理）',
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
  PRIMARY KEY (`alert_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险预警表';

-- 6.3.19 AI记账记录表
CREATE TABLE IF NOT EXISTS `bookkeeping` (
  `book_id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '记账ID',
  `user_id`              BIGINT        NOT NULL COMMENT '用户ID',
  `trans_id`             BIGINT        NOT NULL COMMENT '关联交易ID',
  `auto_category`        VARCHAR(16)   NOT NULL COMMENT '自动分类',
  `confirm_status`       TINYINT       NOT NULL DEFAULT 0 COMMENT '确认状态（0自动/1待确认/2已确认）',
  `cashflow_report_flag` TINYINT       NOT NULL DEFAULT 0 COMMENT '现金流报表标识（0否/1是）',
  `profit_estimate`      DECIMAL(12,2) NULL COMMENT '简易利润测算',
  `book_date`            DATE          NOT NULL COMMENT '记账日期',
  PRIMARY KEY (`book_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI记账记录表';

-- 6.3.20 政策信息表
CREATE TABLE IF NOT EXISTS `policy_info` (
  `policy_id`      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '政策ID',
  `policy_name`    VARCHAR(128) NOT NULL COMMENT '政策名称',
  `policy_type`    VARCHAR(32)  NOT NULL COMMENT '政策类型（创业担保贷款/财政贴息/安居/就业扶持）',
  `target_group`   VARCHAR(255) NOT NULL COMMENT '适用对象',
  `amount_limit`   VARCHAR(64)  NULL COMMENT '额度上限',
  `term_limit`     VARCHAR(64)  NULL COMMENT '期限',
  `apply_url`      VARCHAR(500) NOT NULL COMMENT '申报入口地址',
  `effective_date` DATE         NOT NULL COMMENT '生效日期',
  `expire_date`    DATE         NULL COMMENT '失效日期',
  `status`         TINYINT      NOT NULL DEFAULT 0 COMMENT '状态（0有效/1失效）',
  PRIMARY KEY (`policy_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='政策信息表';

-- 6.3.21 政策推送记录表
CREATE TABLE IF NOT EXISTS `policy_push` (
  `push_id`      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '推送ID',
  `policy_id`    BIGINT       NOT NULL COMMENT '政策ID',
  `user_id`      BIGINT       NOT NULL COMMENT '用户ID',
  `push_channel` VARCHAR(16)  NOT NULL COMMENT '推送渠道',
  `push_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '推送时间',
  `click_status` TINYINT      NOT NULL DEFAULT 0 COMMENT '点击状态（0未点击/1已点击）',
  PRIMARY KEY (`push_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='政策推送记录表';

-- 6.3.22 保险代销记录表
CREATE TABLE IF NOT EXISTS `insurance_sales` (
  `insurance_id`      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '代销记录ID',
  `user_id`           BIGINT       NOT NULL COMMENT '用户ID',
  `insurance_type`    VARCHAR(32)  NOT NULL COMMENT '保险类型（履约保证保险/知识产权保险）',
  `insurance_company` VARCHAR(128) NOT NULL COMMENT '承保保险公司',
  `recommend_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '推荐时间',
  `apply_url`         VARCHAR(500) NULL COMMENT '投保跳转地址',
  `channel_code`      VARCHAR(32)  NOT NULL COMMENT '专属渠道编码',
  `insure_status`     TINYINT      NOT NULL DEFAULT 0 COMMENT '投保状态（0未投保/1已投保）',
  `order_callback`    TINYINT      NOT NULL DEFAULT 0 COMMENT '订单回传状态（0未回传/1已回传）',
  `commission_status` TINYINT      NOT NULL DEFAULT 0 COMMENT '佣金结算状态（0未结算/1已结算）',
  PRIMARY KEY (`insurance_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='保险代销记录表';

-- 6.3.23 反诈学习记录表
CREATE TABLE IF NOT EXISTS `anti_fraud_record` (
  `record_id`     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '学习记录ID',
  `user_id`       BIGINT       NOT NULL COMMENT '用户ID',
  `learn_type`    VARCHAR(32)  NOT NULL COMMENT '学习类型（情景模拟/财商课程）',
  `content_id`    VARCHAR(64)  NOT NULL COMMENT '学习内容ID',
  `finish_status` TINYINT      NOT NULL DEFAULT 0 COMMENT '完成状态（0未完成/1已完成）',
  `score`         INT          NULL COMMENT '学习得分',
  `learn_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '学习时间',
  PRIMARY KEY (`record_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='反诈学习记录表';

-- 6.3.24 消息提醒表
CREATE TABLE IF NOT EXISTS `message` (
  `msg_id`       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `user_id`      BIGINT       NOT NULL COMMENT '用户ID',
  `msg_type`     VARCHAR(32)  NOT NULL COMMENT '提醒类型（预算提醒/风险预警/政策推送/业务通知）',
  `msg_content`  VARCHAR(500) NOT NULL COMMENT '提醒内容',
  `push_channel` VARCHAR(16)  NOT NULL DEFAULT '站内' COMMENT '推送渠道',
  `send_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `read_status`  TINYINT      NOT NULL DEFAULT 0 COMMENT '已读状态（0未读/1已读）',
  PRIMARY KEY (`msg_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息提醒表';

-- 6.3.25 青年画像标签表
CREATE TABLE IF NOT EXISTS `profile_tag` (
  `tag_id`      BIGINT        NOT NULL AUTO_INCREMENT COMMENT '画像标签ID',
  `user_id`     BIGINT        NOT NULL COMMENT '用户ID',
  `dim_code`    VARCHAR(32)   NOT NULL COMMENT '画像维度（安居稳定性/经营能力/资金健康度/履约记录）',
  `tag_content` VARCHAR(255)  NOT NULL COMMENT '标签内容',
  `confidence`  DECIMAL(5,4)  NOT NULL COMMENT '置信度',
  `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`tag_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='青年画像标签表';
