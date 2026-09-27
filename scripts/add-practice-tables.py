# -*- coding: utf-8 -*-
# schema.sql 追加 L3 反诈对话演练两张表，并更新表数量校验注释
import io

path = r'D:\codex\codex-data\qingqi-ecity\backend\sql\schema.sql'
s = io.open(path, 'r', encoding='utf-8').read()

tables = '''
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
'''

old_tail = '''-- =============================================================
-- 表数量校验：共 30 张表（原25 + biz_policy + biz_credit_txn + biz_insurance_product + biz_finance_product + biz_risk_assessment）
-- =============================================================
'''
assert old_tail in s, 'tail anchor not found'
s = s.replace(old_tail, tables)
io.open(path, 'w', encoding='utf-8', newline='').write(s)
print('schema.sql updated, now', s.count('CREATE TABLE `'), 'CREATE TABLE statements')
