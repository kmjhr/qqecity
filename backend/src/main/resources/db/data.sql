-- =============================================================
-- 青启e城 演示系统 演示数据
-- 所有数据均为演示用模拟数据，不构成真实金融服务
-- =============================================================
USE qingqi;

-- 演示用户：13800000000 / 123456（SHA-256 散列）
INSERT INTO `youth_user` (`user_id`, `name`, `id_type`, `id_number`, `phone`, `user_type`, `school`, `edu_level`, `grad_year`, `password_hash`)
VALUES (1, '演示青年', '身份证', '440000199901010010', '13800000000', '应届毕业生', '演示大学', '本科', '2026', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92');

-- 演示消息
INSERT INTO `message` (`user_id`, `msg_type`, `msg_content`, `push_channel`) VALUES
(1, '业务通知', '欢迎使用青启e城演示系统，请完善人群资质信息以体验保函与青创e贷服务。', '站内'),
(1, '政策推送', '您可能符合创业担保贷款申报条件（演示政策），请前往政策中心查看。', '站内');

-- 演示政策
INSERT INTO `policy_info` (`policy_name`, `policy_type`, `target_group`, `amount_limit`, `term_limit`, `apply_url`, `effective_date`, `expire_date`, `status`) VALUES
('创业担保贷款（演示）', '创业担保贷款', '在校大学生、毕业2年内应届生', '最高30万元（部分地区提额至50万元）', '最长3年', 'https://example.com/policy/1', '2026-01-01', '2027-12-31', 0),
('人才安居补贴（演示）', '安居', '应届毕业生', '以当地公告为准', '—', 'https://example.com/policy/2', '2026-01-01', '2027-12-31', 0),
('财政贴息新政（演示）', '财政贴息', '中小微民营企业、服务业经营主体', '以当年财政公告为准', '—', 'https://example.com/policy/3', '2026-01-01', '2027-12-31', 0);

-- 演示预算（当月）
INSERT INTO `budget` (`user_id`, `budget_month`, `category`, `budget_amount`, `used_amount`, `remain_ratio`, `remind_level`, `status`) VALUES
(1, '2026-09', '餐饮', 1500.00, 780.00, 48.00, 1, 0),
(1, '2026-09', '购物', 800.00, 700.00, 12.50, 2, 0);

-- 演示贷款申请（B 类已预审）
INSERT INTO `loan_apply` (`user_id`, `loan_type`, `apply_amount`, `loan_purpose`, `precheck_status`, `precheck_range`, `approve_status`) VALUES
(1, 'B', 15000.00, '市集摊位物料采购（演示）', 1, '10000-20000', 0);

-- 演示授信额度（A 类样例）
INSERT INTO `credit_limit` (`user_id`, `credit_type`, `credit_amount`, `used_amount`, `remain_amount`, `valid_start`, `valid_end`, `credit_status`) VALUES
(1, 'A', 50000.00, 12000.00, 38000.00, '2026-09-01', '2027-09-01', 0);
