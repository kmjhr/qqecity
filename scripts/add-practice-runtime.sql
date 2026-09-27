-- L3 反诈对话演练：运行库建表（幂等）+ 4 个 SCENARIO_DIALOG 剧本补灌
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `biz_scenario_practice` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT,
  `practice_no`    VARCHAR(50)  NOT NULL,
  `user_id`        BIGINT       NOT NULL,
  `scenario_id`    BIGINT       NOT NULL,
  `scenario_title` VARCHAR(200) NULL,
  `round_count`    INT          NOT NULL DEFAULT 0,
  `result`         VARCHAR(20)  NOT NULL,
  `risk_score`     INT          NOT NULL DEFAULT 0,
  `result_desc`    VARCHAR(800) NULL,
  `started_at`     DATETIME     NULL,
  `ended_at`       DATETIME     NULL,
  `deleted`        TINYINT      NOT NULL DEFAULT 0,
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_practice_no` (`practice_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_scenario_id` (`scenario_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='反诈对话演练记录表（演示，模拟）';

CREATE TABLE IF NOT EXISTS `biz_scenario_round` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT,
  `practice_id` BIGINT        NOT NULL,
  `round_no`    INT           NOT NULL,
  `speaker`     VARCHAR(20)   NOT NULL,
  `content`     VARCHAR(1000) NOT NULL,
  `safe_score`  INT           NULL,
  `hit_words`   VARCHAR(500)  NULL,
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_practice_id` (`practice_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='反诈对话演练回合明细表（演示，模拟）';

INSERT IGNORE INTO `biz_anti_fraud_content`
(`id`, `title`, `content_type`, `category`, `summary`, `content`, `view_count`, `sort_order`, `status`, `publish_time`) VALUES
(8, '对话演练·刷单返利诈骗', 'SCENARIO_DIALOG', '刷单诈骗', 'AI 扮演诈骗分子：从点赞返利到垫资收割，实战应对刷单骗局', '{"background":"大三学生小陈课少想找兼职，校园群里看到「学长推荐刷单兼职，日赚200元」的广告。加对方微信后做了两个点赞任务，各返8元、15元，返利秒到账。现在对方让你下载App做「垫资任务」。","opening":"亲，点赞任务你都完成了，返利也到账了吧？现在有个高级任务：购物垫付返20%，一单几百块，稳赚不赔！","turns":[{"no":1,"fraud":"下载这个App注册，先垫200元试一单，马上返240元给你。"},{"no":2,"fraud":"你刚才操作失误卡单了，系统要冻结，需要再充800元解冻费才能继续。"},{"no":3,"fraud":"解冻后本金佣金一起返，就差最后一步了，别半途而废，再充2000元就能提现。"}],"safeWords":["96110","110","挂断","核实","报警","不转账","不垫资","官方","拉黑"],"dangerWords":["垫资","转账","解冻","保证金","卡单","充值","验证码"]}', 0, 20, 1, '2026-09-27 10:00:00'),
(9, '对话演练·冒充公检法', 'SCENARIO_DIALOG', '冒充公检法', 'AI 扮演骗子：从权威施压到安全账户，识别线上办案骗局', '{"background":"小陈接到一个来电显示为「96110」的电话，对方自称市反诈中心民警，准确报出他的姓名和身份证号，说他名下手机卡发送了300余条诈骗短信、涉嫌一起重大洗钱案，要求全程保密配合调查，并通过聊天软件发来一张带公章的「逮捕令」。","opening":"你是小陈吧？我是市反诈中心的民警，你名下一张手机卡涉嫌发送诈骗短信300余条，涉及一起重大洗钱案，现在必须配合我们线上调查。","turns":[{"no":1,"fraud":"案件涉密，全程不许挂电话、不许告诉任何人，否则我们立即上门拘捕。"},{"no":2,"fraud":"下载这个「安全防护」App并开启屏幕共享，我们对你的账户资金做清查。"},{"no":3,"fraud":"清查发现你的账户有涉案资金，必须转到我们的「安全账户」验资，24小时后自动返还。"}],"safeWords":["96110","110","挂断","派出所","核实","报警","当面","官方","不转账"],"dangerWords":["安全账户","转账","共享屏幕","验证码","下载","保密"]}', 0, 21, 1, '2026-09-27 10:00:00'),
(10, '对话演练·虚假征信注销账户', 'SCENARIO_DIALOG', '征信洗白', 'AI 扮演平台客服：以影响征信施压，识破注销账户骗局', '{"background":"小陈接到自称「某金融平台客服」的电话，说他大学期间注册过一个贷款账户，现在国家政策要求注销，不注销会影响征信、影响以后买房买车，对方还准确说出他的学校和姓名。","opening":"您好，我是XX金融平台的客服，查询到您大学期间注册过一个贷款账户，现在国家政策要求注销，不注销会影响您的个人征信。","turns":[{"no":1,"fraud":"您名下这个校园贷账户一直没注销，征信马上就要出不良记录了，以后买房买车都会受影响。"},{"no":2,"fraud":"需要把平台上的贷款额度全部提出来，转到我们的「清算账户」验资，验完立即返还并注销账户。"},{"no":3,"fraud":"再不处理，平台每月要收3000元违约金，还会直接拉黑您的征信。"}],"safeWords":["96110","110","挂断","核实","官方","征信中心","不转账","免费"],"dangerWords":["清空额度","清算账户","转账","违约金","验证码","贷款"]}', 0, 22, 1, '2026-09-27 10:00:00'),
(11, '对话演练·AI换脸冒充熟人', 'SCENARIO_DIALOG', 'AI换脸', 'AI 扮演换脸「老同学」：视频通话求救，识破杀熟骗局', '{"background":"小陈微信收到「老同学」的好友申请，头像昵称都对。对方发起视频通话，画面里确实是他同学的脸，但信号「卡顿」声音听不清，随后文字发来：「我在医院急诊，差2万押金，先借我，回头还你。」","opening":"老同学，我手机丢了换号了，你微信通过一下，急事找你！","turns":[{"no":1,"fraud":"我在医院急诊，差2万押金，你先借我周转一下，回头一定还你。"},{"no":2,"fraud":"视频信号不好听不清是吧？没事，我把收款账户发你，你直接转这个就行。"},{"no":3,"fraud":"真急用，钱不到位人就没了，你看着办，别耽误了。"}],"safeWords":["核实","原号码","当面","约定口令","家人","报警","不转账","视频验证"],"dangerWords":["转账","新账户","收款码","验证码","别告诉"]}', 0, 23, 1, '2026-09-27 10:00:00');
