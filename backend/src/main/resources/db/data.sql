-- =============================================================
-- 青启e城 演示数据 data.sql
-- 所有数据均为演示用模拟数据，不构成真实金融服务
-- 幂等设计：使用 INSERT IGNORE，重复导入不会报错也不会重复
-- 密码算法：BCrypt（与后端 SysUserService 一致）
-- 默认密码：123456
-- =============================================================
USE qingqi;

-- =============================================================
-- 一、用户与权限（5张表）
-- =============================================================

-- 1. sys_role 角色表
INSERT IGNORE INTO `sys_role` (`id`, `role_code`, `role_name`, `description`, `sort_order`, `status`) VALUES
(1, 'ADMIN', '系统管理员', '拥有系统全部权限', 1, 1),
(2, 'USER', '青年用户', '普通青年用户，使用业务功能', 10, 1),
(3, 'LANDLORD', '房东', '保函受益人，可确认保函、发起索赔', 20, 1),
(4, 'BANK_OPERATOR', '银行运营岗', '人工复核保函索赔、贷款审核', 30, 1);

-- 2. sys_user 用户表
-- 密码 123456 的 BCrypt 哈希值：$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2
INSERT IGNORE INTO `sys_user` (`id`, `username`, `password`, `nickname`, `real_name`, `id_card`, `phone`, `email`, `role`, `user_type`, `status`) VALUES
(1, 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '系统管理员', '管小明', NULL, '13800000000', 'admin@example.com', 'ADMIN', 'OTHER', 1),
(2, 'testuser', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '演示青年', '张青年', NULL, '13900000001', 'test@example.com', 'USER', 'STUDENT', 1),
(3, 'entrepreneur', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '创业小青年', '李创业', NULL, '13900000002', 'biz@example.com', 'USER', 'ENTREPRENEUR', 1),
(4, 'landlord01', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '王房东', '王建国', NULL, '13900000003', 'landlord@example.com', 'LANDLORD', 'OTHER', 1),
(5, 'banker01', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '银行运营', '赵经理', NULL, '13900000004', 'bank@example.com', 'BANK_OPERATOR', 'OTHER', 1);

-- 3. sys_user_role 用户角色关联
INSERT IGNORE INTO `sys_user_role` (`id`, `user_id`, `role_id`) VALUES
(1, 1, 1),
(2, 2, 2),
(3, 3, 2),
(4, 4, 3),
(5, 5, 4);

-- 4. sys_login_log 登录日志（示例数据）
INSERT IGNORE INTO `sys_login_log` (`id`, `user_id`, `username`, `login_ip`, `login_type`, `status`, `fail_reason`, `user_agent`) VALUES
(1, 2, 'testuser', '127.0.0.1', 'PASSWORD', 1, NULL, 'Mozilla/5.0 Windows NT 10.0'),
(2, 3, 'entrepreneur', '127.0.0.1', 'PASSWORD', 1, NULL, 'Mozilla/5.0 Macintosh'),
(3, NULL, 'wronguser', '127.0.0.1', 'PASSWORD', 0, '用户不存在', 'Mozilla/5.0');

-- 5. sys_message 消息表
INSERT IGNORE INTO `sys_message` (`id`, `user_id`, `title`, `content`, `type`, `biz_type`, `is_read`) VALUES
(1, 2, '欢迎使用青启e城', '欢迎使用青启e城演示系统！完善您的人群资质信息，即可体验安居保函、青创e贷等特色服务。', 'SYSTEM', NULL, 0),
(2, 2, '预算提醒', '您本月餐饮类预算已使用78%，请注意控制消费。', 'BUDGET', NULL, 0),
(3, 2, '创业政策推送', '您可能符合创业担保贷款申报条件（演示政策），请前往政策中心查看详情。', 'BUSINESS', 'LOAN', 1),
(4, 2, '系统通知', '系统将于本周日凌晨进行例行维护，预计持续1小时。', 'SYSTEM', NULL, 1),
(5, 3, '欢迎加入青创计划', '欢迎加入青创e贷计划！上传您的经营流水，即可获取智能预授信额度。', 'SYSTEM', NULL, 0),
(6, 3, '业务通知', '您的贷款申请已通过预审，预授信额度范围：10000-20000元。', 'BUSINESS', 'LOAN', 0),
(7, 4, '保函待确认', '您有一笔新的保函申请待确认，请及时处理。', 'BUSINESS', 'GUARANTEE', 0);

-- =============================================================
-- 二、安居金融风控（6张表）
-- =============================================================

-- 6. biz_landlord 房东信息
INSERT IGNORE INTO `biz_landlord` (`id`, `user_id`, `real_name`, `id_card`, `phone`, `bank_account`, `bank_name`, `verify_status`) VALUES
(1, 4, '王建国', NULL, '13900000003', '6222021234567890123', '中国工商银行杭州分行', 'VERIFIED');

-- 7. biz_house 房屋信息
INSERT IGNORE INTO `biz_house` (`id`, `landlord_id`, `house_title`, `province`, `city`, `district`, `address`, `house_type`, `area`, `room_count`, `monthly_rent`, `deposit_amount`, `status`) VALUES
(1, 1, '西湖区精装一室一厅近地铁', '浙江省', '杭州市', '西湖区', '西湖区文一西路123号', 'APARTMENT', 45.50, 1, 2800.00, 2800.00, 1),
(2, 1, '余杭区未来科技城两居室', '浙江省', '杭州市', '余杭区', '余杭区文一西路969号', 'APARTMENT', 78.00, 2, 4200.00, 4200.00, 1);

-- 8. biz_rental_contract 租赁合同
INSERT IGNORE INTO `biz_rental_contract` (`id`, `contract_no`, `house_id`, `landlord_id`, `tenant_id`, `monthly_rent`, `deposit_amount`, `rent_start_date`, `rent_end_date`, `pay_method`, `contract_status`, `sign_date`) VALUES
(1, 'HT202401001', 1, 1, 2, 2800.00, 2800.00, '2024-01-15', '2025-01-14', 'MONTHLY', 'SIGNED', '2024-01-10'),
(2, 'HT202402001', 2, 1, 3, 4200.00, 4200.00, '2024-02-01', '2025-01-31', 'QUARTERLY', 'SIGNED', '2024-01-20');

-- 9. biz_guarantee_application 保函申请
INSERT IGNORE INTO `biz_guarantee_application` (`id`, `apply_no`, `tenant_id`, `house_id`, `contract_id`, `landlord_id`, `deposit_amount`, `guarantee_rate`, `guarantee_fee`, `guarantee_period_months`, `applicant_name`, `applicant_phone`, `landlord_name`, `landlord_phone`, `apply_status`, `ai_review_result`, `ai_review_score`, `submit_time`, `landlord_confirm_time`, `review_time`) VALUES
(1, 'GBA202401001', 2, 1, 1, 1, 2800.00, 0.0100, 28.00, 12, '张青年', '13900000001', '王建国', '13900000003', 'APPROVED', 'PASS', 92, '2024-01-10 10:30:00', '2024-01-11 09:00:00', '2024-01-11 14:00:00'),
(2, 'GBA202402001', 3, 2, 2, 1, 4200.00, 0.0120, 50.40, 12, '李创业', '13900000002', '王建国', '13900000003', 'LANDLORD_CONFIRM', NULL, NULL, '2024-02-01 14:00:00', NULL, NULL);

-- 10. biz_guarantee 保函主表
INSERT IGNORE INTO `biz_guarantee` (`id`, `guarantee_no`, `application_id`, `tenant_id`, `landlord_id`, `house_id`, `guarantee_amount`, `guarantee_fee`, `effective_date`, `expire_date`, `guarantee_status`, `pay_status`, `pay_time`, `issue_time`) VALUES
(1, 'GB2024010001', 1, 2, 1, 1, 2800.00, 28.00, '2024-01-15', '2025-01-14', 'ACTIVE', 'PAID', '2024-01-12 10:00:00', '2024-01-12 10:00:00');

-- 11. biz_guarantee_claim 索赔表（空数据，演示无索赔案例）
-- 暂无演示索赔数据

-- =============================================================
-- 三、青创e贷（4张表）
-- =============================================================

-- 12. biz_merchant 商户信息
INSERT IGNORE INTO `biz_merchant` (`id`, `merchant_name`, `merchant_type`, `contact_name`, `contact_phone`, `address`, `business_license`, `bank_account`, `bank_name`, `verify_status`, `status`) VALUES
(1, '杭州文创物料供应商', 'MATERIAL', '陈经理', '0571-88881234', '杭州市拱墅区祥园路1号', '91330100MA2XXXXXX', '6222021234567890456', '中国工商银行杭州拱宸支行', 'VERIFIED', 1),
(2, '梦想市集摊位管理方', 'STALL', '周主管', '0571-88885678', '杭州市西湖区文三路478号', '91330100MA2YYYYYY', '6222021234567890789', '中国工商银行杭州文三路支行', 'VERIFIED', 1),
(3, '小红书推广服务中心', 'PROMOTION', '吴专员', '0571-88889012', '杭州市滨江区网商路599号', '91330100MA2ZZZZZZ', '6222021234567890012', '中国工商银行杭州滨江支行', 'VERIFIED', 1),
(4, '青年创业孵化基地', 'OTHER', '孙老师', '0571-88883456', '杭州市余杭区梦想小镇', '91330100MA2AAAAAA', '6222021234567890345', '中国工商银行杭州余杭支行', 'VERIFIED', 1);

-- 13. biz_loan_application 贷款申请
INSERT IGNORE INTO `biz_loan_application` (`id`, `apply_no`, `user_id`, `loan_type`, `apply_amount`, `purpose`, `business_plan`, `crowd_type`, `pre_check_result`, `pre_check_min_amount`, `pre_check_max_amount`, `apply_status`, `approve_amount`, `submit_time`, `approve_time`) VALUES
(1, 'LA202402001', 3, 'B_TYPE', 20000.00, '进货采购文创产品', '计划在梦想市集开设文创摊位，主营手工艺品，预计月营收8000-15000元。', 'ENTREPRENEUR', 'ELIGIBLE', 10000.00, 20000.00, 'APPROVED', 20000.00, '2024-02-05 10:00:00', '2024-02-06 14:00:00'),
(2, 'LA202403001', 2, 'B_TYPE', NULL, NULL, '在校大学生，计划毕业创业，先做免费预审了解额度。', 'STUDENT', 'ELIGIBLE', 5000.00, 10000.00, 'PRE_CHECK', NULL, '2024-03-10 16:00:00', NULL);

-- 14. biz_credit_limit 授信额度
INSERT IGNORE INTO `biz_credit_limit` (`id`, `user_id`, `credit_type`, `total_limit`, `used_limit`, `available_limit`, `interest_rate`, `status`, `effective_date`, `expire_date`) VALUES
(1, 3, 'B_TYPE', 20000.00, 15000.00, 5000.00, 0.0435, 'ACTIVE', '2024-02-10', '2025-02-09');

-- 15. biz_entrust_payment 受托支付
INSERT IGNORE INTO `biz_entrust_payment` (`id`, `payment_no`, `user_id`, `loan_application_id`, `merchant_id`, `merchant_name`, `amount`, `purpose`, `payment_status`, `payment_time`) VALUES
(1, 'EP202402001', 3, 1, 1, '杭州文创物料供应商', 10000.00, '文创产品首批进货', 'SUCCESS', '2024-02-15 10:30:00'),
(2, 'EP202403001', 3, 1, 2, '梦想市集摊位管理方', 5000.00, '摊位租金押金', 'SUCCESS', '2024-03-01 09:00:00');

-- =============================================================
-- 四、经营赋能（2张表）
-- =============================================================

-- 16. biz_bookkeeping_record 记账记录（演示数据）
INSERT IGNORE INTO `biz_bookkeeping_record` (`id`, `user_id`, `record_type`, `category`, `amount`, `happen_date`, `description`, `source`, `is_confirmed`, `related_party`) VALUES
(1, 3, 'EXPENSE', '进货成本', 6500.00, '2024-02-20', '首批文创产品进货', 'MANUAL', 1, '杭州文创物料供应商'),
(2, 3, 'EXPENSE', '场地租金', 2500.00, '2024-03-01', '3月份摊位租金', 'MANUAL', 1, '梦想市集'),
(3, 3, 'INCOME', '销售收入', 8600.00, '2024-03-15', '3月上半月销售收入', 'AUTO', 1, '零售顾客'),
(4, 3, 'EXPENSE', '营销推广', 800.00, '2024-03-10', '小红书推广费', 'MANUAL', 1, '小红书推广服务中心'),
(5, 3, 'INCOME', '销售收入', 9200.00, '2024-03-31', '3月下半月销售收入', 'AUTO', 1, '零售顾客');

-- 17. biz_cashflow_report 现金流报表（演示数据）
INSERT IGNORE INTO `biz_cashflow_report` (`id`, `user_id`, `report_period`, `total_income`, `total_expense`, `net_cash_flow`, `profit_amount`, `profit_margin`, `warning_level`, `warning_content`, `generate_time`) VALUES
(1, 3, '2024-03', 17800.00, 9800.00, 8000.00, 3200.00, 17.98, 'NORMAL', NULL, '2024-04-01 00:00:00');

-- =============================================================
-- 五、预算消费（4张表）
-- =============================================================

-- 18. biz_budget_category 预算分类（系统预设）
INSERT IGNORE INTO `biz_budget_category` (`id`, `category_code`, `category_name`, `category_type`, `sort_order`, `is_system`, `status`) VALUES
(1, 'FOOD', '餐饮美食', 'FOOD', 1, 1, 1),
(2, 'ENTERTAINMENT', '休闲娱乐', 'ENTERTAINMENT', 2, 1, 1),
(3, 'SHOPPING', '购物消费', 'SHOPPING', 3, 1, 1),
(4, 'TRANSPORT', '交通出行', 'TRANSPORT', 4, 1, 1),
(5, 'HOUSING', '居住生活', 'OTHER', 5, 1, 1),
(6, 'STUDY', '学习提升', 'OTHER', 6, 1, 1),
(7, 'HEALTH', '医疗健康', 'OTHER', 7, 1, 1),
(8, 'OTHER', '其他支出', 'OTHER', 99, 1, 1);

-- 19. biz_budget_setting 预算设置
INSERT IGNORE INTO `biz_budget_setting` (`id`, `user_id`, `category_id`, `category_code`, `budget_period`, `budget_amount`, `used_amount`, `remaining_amount`, `usage_percent`, `remind_50_sent`, `remind_20_sent`, `remind_over_sent`) VALUES
(1, 2, 1, 'FOOD', '2024-03', 1500.00, 1170.00, 330.00, 78.00, 1, 0, 0),
(2, 2, 2, 'ENTERTAINMENT', '2024-03', 800.00, 320.00, 480.00, 40.00, 0, 0, 0),
(3, 2, 3, 'SHOPPING', '2024-03', 1000.00, 1050.00, -50.00, 105.00, 1, 1, 1),
(4, 2, 4, 'TRANSPORT', '2024-03', 500.00, 280.00, 220.00, 56.00, 1, 0, 0),
(5, 2, 5, 'HOUSING', '2024-03', 3000.00, 2800.00, 200.00, 93.33, 1, 1, 0);

-- 20. biz_transaction 交易记录（模拟数据）
INSERT IGNORE INTO `biz_transaction` (`id`, `user_id`, `transaction_no`, `transaction_type`, `category_id`, `category_code`, `amount`, `merchant_name`, `mcc_code`, `transaction_time`, `description`, `source`, `budget_id`) VALUES
(1, 2, 'TX20240301001', 'EXPENSE', 1, 'FOOD', 35.00, '麦当劳(文一西路店)', '5814', '2024-03-01 12:30:00', '午餐', 'SIMULATED', 1),
(2, 2, 'TX20240302001', 'EXPENSE', 4, 'TRANSPORT', 8.00, '杭州地铁', '4111', '2024-03-02 08:15:00', '地铁出行', 'SIMULATED', 4),
(3, 2, 'TX20240305001', 'EXPENSE', 3, 'SHOPPING', 299.00, '淘宝服饰店', '5699', '2024-03-05 20:00:00', '网购衣服', 'SIMULATED', 3),
(4, 2, 'TX20240308001', 'EXPENSE', 2, 'ENTERTAINMENT', 120.00, '猫眼电影', '7832', '2024-03-08 19:30:00', '看电影', 'SIMULATED', 2),
(5, 2, 'TX20240310001', 'EXPENSE', 1, 'FOOD', 156.00, '海底捞火锅', '5812', '2024-03-10 18:00:00', '朋友聚餐', 'SIMULATED', 1),
(6, 2, 'TX20240312001', 'EXPENSE', 5, 'HOUSING', 2800.00, '房租转账', '6538', '2024-03-12 09:00:00', '3月房租', 'SIMULATED', 5),
(7, 2, 'TX20240315001', 'EXPENSE', 3, 'SHOPPING', 750.00, '京东商城', '5311', '2024-03-15 14:00:00', '数码配件', 'SIMULATED', 3);

-- 21. biz_saving_goal 心愿储蓄
INSERT IGNORE INTO `biz_saving_goal` (`id`, `user_id`, `goal_name`, `target_amount`, `current_amount`, `progress_percent`, `deadline`, `description`, `status`) VALUES
(1, 2, '毕业旅行基金', 5000.00, 1250.00, 25.00, '2024-06-30', '毕业和室友一起去云南旅行', 'ACTIVE'),
(2, 2, '新手机', 3000.00, 800.00, 26.67, '2024-05-01', '换一部新手机', 'ACTIVE'),
(3, 3, '创业启动金', 20000.00, 8000.00, 40.00, '2024-06-01', '创业项目启动资金', 'ACTIVE');

-- =============================================================
-- 六、金融安全（4张表）
-- =============================================================

-- 22. biz_anti_fraud_content 反诈内容
INSERT IGNORE INTO `biz_anti_fraud_content` (`id`, `title`, `content_type`, `category`, `summary`, `content`, `view_count`, `sort_order`, `status`, `publish_time`) VALUES
(1, '警惕"征信修复"骗局：花钱就能洗白征信？', 'ARTICLE', '征信修复', '凡是声称可以"征信修复""征信洗白""征信铲单"的，全是诈骗！', '## 什么是征信修复骗局？\n\n不法分子谎称可以通过"内部渠道""特殊关系"帮你消除征信不良记录，收取高额手续费后消失无踪。\n\n## 常见套路：\n1. 散布"征信修复"广告，精准定位有逾期记录的人群\n2. 声称有"内部人脉"可以"铲单"\n3. 收取高额费用，少则几千多则几万\n4. 拉黑消失，或者教你伪造材料（反而违法）\n\n## 防范要点：\n- 征信领域无"修复"概念，只有"异议申请"的法定渠道\n- 正规异议申请不收费，可通过人民银行征信中心办理\n- 凡是提前收费的都是诈骗', 1280, 1, 1, '2024-01-15 10:00:00'),
(2, '大学生必看：校园贷的十大套路', 'ARTICLE', '套路贷', '校园贷变种繁多，零门槛、低利息背后是万丈深渊。', '## 校园贷常见套路：\n\n1. "零门槛、秒到账"——实际利率远超法定上限\n2. "裸条借贷"——用不雅照片做抵押\n3. "培训贷"——以招聘培训为名诱导贷款\n4. "美容贷"——医美机构联合放款\n5. "回租贷"——手机抵押变相高利贷\n6. "刷单贷"——先刷单后骗贷\n7. "传销贷"——拉人头做贷款\n8. "套路贷"——故意制造违约逼债\n9. "考证贷"——以考证为名骗取贷款\n10. "创业贷"——虚假创业项目骗贷\n\n## 如何防范：\n- 树立正确消费观，不攀比不超前消费\n- 确有资金需求找正规金融机构\n- 遇到可疑情况及时告诉老师家长', 2156, 2, 1, '2024-01-20 10:00:00'),
(3, '租房诈骗高发期！这些套路要警惕', 'ARTICLE', '电信诈骗', '租房季来临，虚假房源、押金诈骗层出不穷。', '## 租房常见诈骗套路：\n\n1. **虚假房源**：网上图片精美、价格低廉，实际根本不存在，骗你交"看房费""定金"\n2. **二房东骗局**：冒充房东出租，收了房租就跑路\n3. **黑中介**：收取高额中介费后不办事，或诱导签霸王合同\n4. **租金贷陷阱**：看似月付房租，实际是办理了分期贷款\n5. **退租套路**：以各种理由克扣押金\n\n## 防范建议：\n- 选择正规中介平台\n- 看房前不要交任何费用\n- 核实房东房产证和身份证\n- 合同条款逐条看清，特别是退租条款\n- 凡是要求贷款付房租的，坚决拒绝', 980, 3, 1, '2024-02-01 10:00:00'),
(4, '反诈小测试：你能识别这些诈骗话术吗？', 'ARTICLE', '电信诈骗', '快来测测你的反诈意识等级！', '## 测试题：\n\n1. "您好，我是公安局的，您涉嫌洗钱犯罪，请将资金转入安全账户接受核查。"\n   → 假！公检法从无"安全账户"概念\n\n2. "恭喜您中奖了，奖品是一台手机，只需缴纳299元税费即可领取。"\n   → 假！正规中奖不收费\n\n3. "您的快递丢失了，我们可以双倍赔偿，请点击链接填写收款信息。"\n   → 假！这是钓鱼链接，套取你的银行卡信息\n\n4. "学长推荐的兼职，日赚300不是梦，只需先交押金入职。"\n   → 假！先交钱的兼职都是诈骗\n\n5. "老师/领导让你加QQ，有急事需要你帮忙转账。"\n   → 假！这是冒充领导熟人诈骗\n\n## 记住"三不一多"原则：\n- 未知链接不点击\n- 陌生来电不轻信\n- 个人信息不透露\n- 转账汇款多核实', 3420, 4, 1, '2024-02-10 10:00:00');

-- 23. biz_fraud_detection_log 骗局甄别记录（演示数据）
INSERT IGNORE INTO `biz_fraud_detection_log` (`id`, `user_id`, `input_text`, `detect_result`, `risk_level`, `matched_rules`, `warning_content`, `detect_time`) VALUES
(1, 2, '我认识银行内部的人，可以帮你消除征信不良记录，只要5000块手续费，保证洗白。', 'DANGEROUS', 5, '["征信修复关键词","内部渠道关键词","收费承诺"]', '高度疑似"征信修复"骗局！征信领域不存在"修复""洗白"概念，任何声称可以花钱消除不良记录的都是诈骗。正规渠道是向人民银行征信中心提出异议申请，且不收取任何费用。', '2024-03-08 15:30:00'),
(2, 2, '学长推荐的刷单兼职，一天能赚200块，先交100块押金入职，一周就回本。', 'DANGEROUS', 5, '["刷单关键词","先交押金","高收益承诺"]', '高度疑似刷单诈骗！所有要求先交钱的"刷单兼职"都是诈骗。刷单本身就是违法行为，切勿参与。', '2024-03-12 11:00:00'),
(3, 2, '你好，我是工商银行客服，您的信用卡需要升级，麻烦提供一下短信验证码。', 'SUSPICIOUS', 3, '["索要验证码","冒充银行客服"]', '可疑！银行工作人员绝不会索要你的短信验证码。任何索要验证码的行为都是诈骗，请直接挂断并拨打官方客服电话核实。', '2024-03-15 09:45:00');

-- 24. biz_credit_report 征信报告（演示数据）
INSERT IGNORE INTO `biz_credit_report` (`id`, `user_id`, `report_no`, `report_type`, `credit_score`, `credit_level`, `total_loan_count`, `overdue_count`, `total_credit_limit`, `used_credit_limit`, `query_count`, `report_summary`, `query_time`, `source`) VALUES
(1, 2, 'CR20240301001', 'SIMPLE', 720, 'GOOD', 0, 0, 15000.00, 0.00, 3, '信用状况良好，无逾期记录，建议保持良好的信用习惯。', '2024-03-01 10:00:00', 'SIMULATED'),
(2, 3, 'CR20240301002', 'DETAIL', 680, 'FAIR', 2, 0, 25000.00, 15000.00, 8, '信用状况一般，有两笔经营类贷款，使用率较高，建议适度控制负债水平。', '2024-03-01 11:00:00', 'SIMULATED');

-- 25. biz_risk_warning 风险预警（演示数据）
INSERT IGNORE INTO `biz_risk_warning` (`id`, `user_id`, `warning_type`, `warning_level`, `warning_title`, `warning_content`, `related_module`, `is_read`, `is_handled`, `warning_time`) VALUES
(1, 2, 'BUDGET_OVER', 'MEDIUM', '购物类预算超支', '您本月购物类预算已超支50元，建议下月合理规划。', 'budget', 1, 0, '2024-03-15 10:00:00'),
(2, 2, 'CREDIT_ABNORMAL', 'LOW', '征信查询次数偏多', '近期您的征信报告被查询3次，请注意保护个人信用，谨慎授权查询。', 'safety', 0, 0, '2024-03-02 10:00:00'),
(3, 3, 'CASHFLOW_WARNING', 'WARNING', '现金流风险预警', '您本月账面收入17800元，但经营支出占比较高，请注意现金流健康。建议预留至少3个月运营资金作为安全垫。', 'bookkeeping', 0, 0, '2024-04-01 09:00:00'),
(4, 3, 'HIGH_FREQ_BORROW', 'MEDIUM', '信贷使用率偏高', '您的B类授信额度使用率已达75%，建议适度控制融资规模，避免负债过高。', 'loan', 0, 0, '2024-03-20 10:00:00');

-- =============================================================
-- 数据导入完成
-- 密码：123456（所有演示账号统一密码）
-- 账号列表：
--   admin / 123456        系统管理员
--   testuser / 123456     演示青年用户（在校生）
--   entrepreneur / 123456 青年创业者
--   landlord01 / 123456   房东账号
--   banker01 / 123456     银行运营岗
-- =============================================================
