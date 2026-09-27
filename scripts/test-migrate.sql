-- 测试期幂等迁移：补齐补充计划新增表与新列（不重建数据卷）
-- 可重复执行

-- ========== 1. biz_credit_limit 观察期 4 列（已执行，跳过）==========
-- ALTER 已在上一步成功，此处不再重复

-- ========== 2. biz_policy 政策库 ==========
CREATE TABLE IF NOT EXISTS `biz_policy` (
  `id`              BIGINT         NOT NULL AUTO_INCREMENT,
  `policy_no`       VARCHAR(50)    NOT NULL,
  `policy_name`     VARCHAR(200)   NOT NULL,
  `policy_type`     VARCHAR(30)    NOT NULL,
  `target_crowd`    VARCHAR(200)   NOT NULL,
  `max_amount`      DECIMAL(18,2)  NULL,
  `subsidy_rate`    VARCHAR(50)    NULL,
  `conditions`      TEXT           NULL,
  `apply_url`       VARCHAR(500)   NULL,
  `policy_source`   VARCHAR(200)   NULL,
  `status`          VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
  `sort_order`      INT            NULL DEFAULT 0,
  `deleted`         TINYINT        NOT NULL DEFAULT 0,
  `create_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_policy_no` (`policy_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT IGNORE INTO `biz_policy` (`policy_no`,`policy_name`,`policy_type`,`target_crowd`,`max_amount`,`subsidy_rate`,`conditions`,`apply_url`,`policy_source`,`sort_order`) VALUES
('ZABZ-001','人才公寓申请','HOUSING','STUDENT,GRADUATE',0.00,'租金减免30%-50%','本科及以上学历；毕业5年内；在本地就业且社保满3月；无自有住房','/api/v1/policy/apply/1','市人力资源和社会保障局',1),
('ZABZ-002','青年落户补贴','HOUSING','GRADUATE',30000.00,'一次性落户补贴最高3万','全日制本科及以上应届毕业生；在本地企业就业；社保连续满6月','/api/v1/policy/apply/2','市公安局',2),
('ZABZ-003','应届毕业生租房补贴','HOUSING','GRADUATE',12000.00,'每月1000元最长12月','毕业2年内；在本地首次就业；家庭人均收入低于上年度人均可支配收入1.5倍','/api/v1/policy/apply/3','市住建局',3),
('ZABZ-004','新就业大学生住房保障','HOUSING','STUDENT,GRADUATE',0.00,'公租房优先配租','大专及以上学历；毕业未满5年；在本地稳定就业；人均住房面积低于16㎡','/api/v1/policy/apply/4','市住房保障中心',4),
('CYTX-001','创业担保贷款（个人最高30万）','ENTREPRENEUR','ENTREPRENEUR,GRADUATE',300000.00,'财政贴息最高3%（LPR-150BP以内部分）','法定劳动年龄内；有具体经营项目；信用良好；参加过创业培训','/api/v1/policy/apply/5','市人社局创业指导科',5),
('CYTX-002','创业担保贷款（部分地区最高50万）','ENTREPRENEUR','ENTREPRENEUR',500000.00,'财政贴息LPR-150BP以内部分','创业担保贷款优质项目；经营满1年；带动就业5人以上；部分地区试点','/api/v1/policy/apply/6','市人社局创业指导科',6),
('CYTX-003','个体工商户税费减免','ENTREPRENEUR','ENTREPRENEUR',0.00,'月销售额10万以内免征增值税','办理个体工商户营业执照；月销售额不超过10万元；小规模纳税人','/api/v1/policy/apply/7','市税务局',7),
('CYTX-004','青年创业场地补贴','ENTREPRENEUR','ENTREPRENEUR,GRADUATE',18000.00,'每年最高6000元最长3年','毕业5年内青年；首次创办经营实体；入驻认定的创业孵化基地','/api/v1/policy/apply/8','市科技局',8);

-- ========== 3. biz_credit_txn 循环贷流水 ==========
CREATE TABLE IF NOT EXISTS `biz_credit_txn` (
  `id`               BIGINT         NOT NULL AUTO_INCREMENT,
  `txn_no`           VARCHAR(50)    NOT NULL,
  `user_id`          BIGINT         NOT NULL,
  `credit_limit_id`  BIGINT         NOT NULL,
  `txn_type`         VARCHAR(20)    NOT NULL,
  `principal_amount` DECIMAL(18,2)  NOT NULL,
  `interest_amount`  DECIMAL(18,2)  NOT NULL DEFAULT 0,
  `borrow_days`      INT            NOT NULL DEFAULT 0,
  `balance_after`    DECIMAL(18,2)  NULL,
  `remark`           VARCHAR(500)   NULL,
  `txn_time`         DATETIME       NOT NULL,
  `deleted`          TINYINT        NOT NULL DEFAULT 0,
  `create_time`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_txn_no` (`txn_no`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 4. biz_insurance_product 保险产品库 ==========
CREATE TABLE IF NOT EXISTS `biz_insurance_product` (
  `id`                BIGINT         NOT NULL AUTO_INCREMENT,
  `product_code`      VARCHAR(50)    NOT NULL,
  `product_name`      VARCHAR(200)   NOT NULL,
  `insurance_type`    VARCHAR(30)    NOT NULL,
  `scene`             VARCHAR(30)    NOT NULL,
  `target_crowd`      VARCHAR(200)   NULL,
  `premium_rate`      VARCHAR(100)   NULL,
  `coverage_amount`   DECIMAL(18,2)  NULL,
  `insurer`           VARCHAR(200)   NOT NULL,
  `product_elements`  TEXT           NULL,
  `conditions`         TEXT           NULL,
  `apply_url`         VARCHAR(500)   NULL,
  `status`            VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
  `sort_order`        INT            NULL DEFAULT 0,
  `deleted`           TINYINT        NOT NULL DEFAULT 0,
  `create_time`       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_code` (`product_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT IGNORE INTO `biz_insurance_product` (`product_code`,`product_name`,`insurance_type`,`scene`,`target_crowd`,`premium_rate`,`coverage_amount`,`insurer`,`product_elements`,`conditions`,`apply_url`,`sort_order`) VALUES
('BX-001','履约保证保险（合同履约版）','PERFORMANCE_BOND','CONTRACT','ENTREPRENEUR,GRADUATE','保费的1.5%-3%（按合同金额分档）',500000.00,'工银安盛人寿（模拟）','险种：履约保证保险；保障范围：被保险人未按合同履约时赔付合同金额；保险期间：与合同期一致；免赔额：合同金额的5%；投保主体：承包方/供应商','签订正式经营合同；合同金额≥5万元；投保人信用良好；经营满6个月','/api/v1/insurance/apply-demo/1',1),
('BX-002','专利执行保险（科技创业版）','IP_PATENT','IP','ENTREPRENEUR','保费的5%-8%（按专利数量分档）',200000.00,'工银安盛人寿（模拟）','险种：知识产权保险-专利执行；保障范围：专利维权诉讼律师费、调查费、取证费；保险期间：1年；免赔额：5000元；投保主体：专利权人','持有有效发明专利或实用新型专利；专利权属无争议；投保前未发生诉讼；经营满1年','/api/v1/insurance/apply-demo/2',2),
('BX-003','侵权责任保险（被诉风险版）','IP_INFRINGEMENT','IP','ENTREPRENEUR','保费的3%-6%（按营业收入分档）',300000.00,'工银安盛人寿（模拟）','险种：知识产权保险-侵权责任；保障范围：被诉侵权时承担的赔偿责任、应诉费用；保险期间：1年；免赔额：1万元；投保主体：经营者','正常经营满1年；近2年无重大侵权诉讼；产品/服务有明确品类；投保人信用良好','/api/v1/insurance/apply-demo/3',3),
('BX-004','小微企业财产综合险','PROPERTY','PROPERTY','ENTREPRENEUR,OTHER','保费的0.8%-1.5%（按资产估值分档）',1000000.00,'工银安盛人寿（模拟）','险种：财产综合险；保障范围：火灾、爆炸、自然灾害造成的财产损失；保险期间：1年；免赔额：2000元；投保主体：经营主体','有固定经营场所；资产估值≥10万元；消防设施合规；经营满6个月','/api/v1/insurance/apply-demo/4',4);

-- ========== 5. biz_finance_product 理财产品库 ==========
CREATE TABLE IF NOT EXISTS `biz_finance_product` (
  `id`                BIGINT         NOT NULL AUTO_INCREMENT,
  `product_code`      VARCHAR(50)    NOT NULL,
  `product_name`      VARCHAR(200)   NOT NULL,
  `product_type`      VARCHAR(30)    NOT NULL,
  `risk_level`        VARCHAR(10)    NOT NULL,
  `expected_return`   VARCHAR(50)    NULL,
  `min_amount`        DECIMAL(18,2)  NULL,
  `period`            VARCHAR(100)   NULL,
  `product_elements`  TEXT           NULL,
  `risk_disclosure`   TEXT           NOT NULL,
  `apply_url`         VARCHAR(500)   NULL,
  `target_risk_level` VARCHAR(100)   NOT NULL,
  `status`            VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
  `sort_order`        INT            NULL DEFAULT 0,
  `deleted`           TINYINT        NOT NULL DEFAULT 0,
  `create_time`       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_code` (`product_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT IGNORE INTO `biz_finance_product` (`product_code`,`product_name`,`product_type`,`risk_level`,`expected_return`,`min_amount`,`period`,`product_elements`,`risk_disclosure`,`apply_url`,`target_risk_level`,`sort_order`) VALUES
('LC-001','心愿储蓄（定期存款模拟）','SAVING_GOAL','R1','1.50%（模拟）',100.00,'灵活存取','类型：储蓄类模拟；起购：100元；存期：灵活；付息：到期还本付息；特点：本金安全、收益稳定（模拟）','存款类产品受存款保险条例保护，本金安全。本演示为模拟收益，实际以银行挂牌利率为准。','/api/v1/finance-product/apply-demo/1','CONSERVATIVE,STEADY,BALANCED',1),
('LC-002','现金管理类产品（模拟）','CASH_MANAGEMENT','R1','2.30%（7日年化，模拟）',1000.00,'T+1 赎回','类型：现金管理类；起购：1000元；赎回：T+1；特点：流动性好、收益波动小（模拟）','本产品为净值型理财，不承诺保本，过往业绩不预示未来表现。投资有风险，理财非存款。','/api/v1/finance-product/apply-demo/2','CONSERVATIVE,STEADY,BALANCED',2),
('LC-003','短债基金（模拟）','SHORT_BOND','R2','3.20%（近一年年化，模拟）',1000.00,'建议持有 6 个月以上','类型：债券型基金；起购：1000元；期限：建议6个月以上；特点：主投短期债券，波动较低（模拟）','基金产品不保本，净值波动可能带来短期浮亏。理财非存款，产品有风险，投资需谨慎。','/api/v1/finance-product/apply-demo/3','STEADY,BALANCED',3),
('LC-004','基金定投（指数型模拟）','FUND_DCA','R2','历史年化 5%-8%（模拟）',100.00,'建议定投 3 年以上','类型：指数基金定投；起购：100元/期；期限：建议3年以上；特点：分散时点、平摊成本（模拟）','基金定投不保本，市场波动可能导致亏损。理财非存款，产品有风险，过往业绩不预示未来。','/api/v1/finance-product/apply-demo/4','STEADY,BALANCED',4),
('LC-005','积存金（黄金定投模拟）','GOLD_ACCUM','R2','随金价波动（模拟）',100.00,'建议持有 1 年以上','类型：黄金积存；起购：100元/期；期限：建议1年以上；特点：分散买入黄金、对抗通胀（模拟）','黄金价格波动较大，积存金不保本不保息。理财非存款，产品有风险，投资需谨慎。','/api/v1/finance-product/apply-demo/5','BALANCED',5);

-- ========== 6. biz_risk_assessment 测评记录 ==========
CREATE TABLE IF NOT EXISTS `biz_risk_assessment` (
  `id`              BIGINT         NOT NULL AUTO_INCREMENT,
  `user_id`         BIGINT         NOT NULL,
  `assess_no`       VARCHAR(50)    NOT NULL,
  `answers`         TEXT           NOT NULL,
  `total_score`     INT            NOT NULL,
  `risk_level`      VARCHAR(20)    NOT NULL,
  `risk_level_name` VARCHAR(20)    NOT NULL,
  `valid_until`     DATE           NOT NULL,
  `is_latest`       TINYINT        NOT NULL DEFAULT 1,
  `assess_time`     DATETIME       NOT NULL,
  `deleted`         TINYINT        NOT NULL DEFAULT 0,
  `create_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_assess_no` (`assess_no`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
