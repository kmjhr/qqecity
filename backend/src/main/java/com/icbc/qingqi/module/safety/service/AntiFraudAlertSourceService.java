package com.icbc.qingqi.module.safety.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icbc.qingqi.module.safety.entity.BizAntiFraudAlert;
import com.icbc.qingqi.module.safety.mapper.BizAntiFraudAlertMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 反诈预警自动源（模块5 · 金融安全「实时反诈预警」）
 * <p>
 * 可配置双模式（项目根 .env 提供，代码只读环境变量占位符，无密钥）：
 * <ul>
 *   <li>source=mock（默认，演示稳定）：定时从模板池轮换生成带当前时间戳的预警，前端轮询刷新即"模拟实时"滚动；</li>
 *   <li>source=real：定时爬取公开反诈信息源（默认中国警察网反诈频道，可用 ANTI_FRAUD_SOURCE_URL 覆盖）；
 *       抓取/解析失败自动回退——表内既有模拟数据继续展示，不空页。</li>
 * </ul>
 * 轮询间隔由 ANTI_FRAUD_POLL_MINUTES 控制（默认 30 分钟）。
 */
@Slf4j
@Component
public class AntiFraudAlertSourceService {

    private final BizAntiFraudAlertMapper alertMapper;

    @Value("${anti-fraud.source:mock}")
    private String source;

    @Value("${anti-fraud.source-url:https://www.cpd.com.cn/n15729503/}")
    private String sourceUrl;

    /** 保留条数上限，超出清理最旧（防止定时生成无限膨胀） */
    private static final int KEEP_MAX = 30;

    /** 自动生成记录的来源标记：仅清理这些来源，人工维护的预警（青启e城·模拟反诈预警 等）永不删除 */
    private static final List<String> AUTO_SOURCES = List.of(
            "平台实时轮换（模拟）", "公开反诈源爬取");

    private final AtomicInteger templateCursor = new AtomicInteger(0);

    /** 模拟实时模板池（与平台五大业务场景强关联：保函/征信/创业贷/理财/平台客服） */
    private static final String[][] TEMPLATES = {
            {"高发", "GUARANTEE", "警惕「免押金保函代办」骗局：交钱后失联",
                    "不法分子冒充平台代办机构，声称可代开租房保函/免押金入住，收取服务费后失联。青启e城保函申请仅通过本平台官方入口办理，不收取任何额外代办费，谨防线下/微信转账代办。"},
            {"高发", "CREDIT", "征信「洗白」「铲单」广告抬头：花钱消除逾期？",
                    "近期征信修复类短信密集出现。征信领域不存在修复/洗白/铲单概念，正规异议申请不收费。凡声称可花钱消除不良记录均为诈骗，请通过人民银行征信中心官方渠道办理。"},
            {"高发", "LOAN", "低息创业贷陷阱：先交「解冻费」才能放款",
                    "冒充银行信贷经理，以\"额度已批\"\"账户冻结需解冻费\"\"刷流水验资\"为由诱导转账。正规银行放款前不收取任何费用，创业贷款请通过银行官方渠道申请。"},
            {"提醒", "WEALTH", "虚假「高收益理财」收割：稳赚不赔都是局",
                    "以内部渠道\"稳赚不赔\"日息千分之X诱导投资虚假理财平台，前期小额返利钓大鱼，大额投入后平台跑路。投资理财请认准持牌机构，收益超过6%需高度警惕。"},
            {"提醒", "PLATFORM", "冒充银行客服「注销贷款账户」骗局多发",
                    "冒充银行客服称\"你的贷款账户影响征信，需注销/关闭\"，诱导下载共享屏幕APP或向\"安全账户\"转账。银行客服不会主动要求注销贷款账户，更不会索要验证码或要求转账。"},
            {"高发", "GUARANTEE", "警惕「押金返现」套路：退租时押金被「扣光」",
                    "部分中介以\"交押金返现\"为饵诱导签约，退租时以各种理由克扣押金。请保留押金收据与房屋交接照片（青启e城支持退租留档），纠纷时向12345/住建部门投诉。"},
            {"提醒", "LOAN", "「校园贷注销」骗局：威胁影响征信要你转账",
                    "不法分子冒充平台客服，称你注册过校园贷需注销，否则影响征信，诱导转账\"清空额度\"。正规平台注销不收费，遇到此类电话请直接挂断并拨打96110。"},
            {"提醒", "CREDIT", "「征信查询次数」骗局：代办\"养征信\"收费",
                    "声称可帮\"养征信\"\"提评分\"收取费用。征信评分由放贷机构综合评定，不存在收费代办。切勿向陌生人提供身份证号、银行卡号及验证码。"},
            {"高发", "PLATFORM", "「共享屏幕」陷阱：客服让你开屏幕共享＝掏空你钱包",
                    "冒充平台客服以\"协助退款/调整利率\"为由诱导开启屏幕共享，远程看到你的验证码与密码。任何情况下不要开启屏幕共享，验证码、密码绝不外泄。"},
            {"提醒", "WEALTH", "「数字人民币」骗局：冒充央行推广\"内测名额\"",
                    "谎称有数字人民币内测名额/高收益钱包，诱导下载仿冒APP。数字人民币是法定货币非投资品，无\"内测收益\"，请通过官方APP使用。"}
    };

    public AntiFraudAlertSourceService(BizAntiFraudAlertMapper alertMapper) {
        this.alertMapper = alertMapper;
    }

    @Scheduled(fixedDelayString = "PT${anti-fraud.poll-minutes:30}M")
    public void poll() {
        try {
            if ("real".equalsIgnoreCase(source)) {
                fetchRealAlerts();
            } else {
                generateMockAlert();
            }
        } catch (Exception e) {
            // 任何异常都不影响预警展示（表内既有数据兜底）
            log.warn("[反诈预警] 自动源执行失败，沿用现有数据：{}", e.getMessage());
        }
    }

    /** 模拟实时：从模板池轮换生成一条当前时间预警，并清理超限旧数据 */
    private void generateMockAlert() {
        int i = templateCursor.getAndIncrement();
        String[] t = TEMPLATES[Math.floorMod(i, TEMPLATES.length)];
        BizAntiFraudAlert a = new BizAntiFraudAlert();
        a.setTitle(t[2]);
        a.setAlertLevel(t[0]);
        a.setRelateScene(t[1]);
        a.setSummary(t[3]);
        a.setRegion("全国");
        a.setSource("平台实时轮换（模拟）");
        a.setPublishTime(LocalDateTime.now());
        a.setStatus(1);
        alertMapper.insert(a);
        trimToMax();
        log.info("[反诈预警] mock 轮换生成：{}（{}）", a.getTitle(), a.getPublishTime());
    }

    /** 真实爬取：抓取公开反诈信息源，解析含"诈/骗/预警"标题入库；失败不抛（回退模拟数据展示） */
    private void fetchRealAlerts() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            HttpRequest req = HttpRequest.newBuilder(URI.create(sourceUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) qingqi-edu-demo")
                    .GET()
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                log.warn("[反诈预警] 抓取源 HTTP {} 不可用，沿用现有数据", resp.statusCode());
                return;
            }
            String html = resp.body();
            Pattern p = Pattern.compile("<a[^>]*href=\"([^\"]+)\"[^>]*>([^<]{6,60})</a>");
            Matcher m = p.matcher(html);
            int added = 0;
            Set<String> seen = new HashSet<>();
            while (m.find() && added < 5) {
                String title = m.group(2).trim();
                String href = m.group(1);
                if (!title.contains("诈") && !title.contains("骗")
                        && !title.contains("预警") && !title.contains("反诈")) {
                    continue;
                }
                if (seen.contains(title)) continue;
                seen.add(title);
                BizAntiFraudAlert a = new BizAntiFraudAlert();
                a.setTitle(title);
                a.setAlertLevel("提醒");
                a.setRelateScene("OTHER");
                a.setSummary("来自公开反诈信息源的实时预警（模拟摘要）：" + title
                        + "。请以官方发布为准，遇骗拨打 96110 / 110。");
                a.setRegion("全国");
                a.setSource("公开反诈源爬取");
                a.setLinkUrl(href.startsWith("http") ? href : sourceUrl);
                a.setPublishTime(LocalDateTime.now());
                a.setStatus(1);
                alertMapper.insert(a);
                added++;
            }
            log.info("[反诈预警] real 爬取完成，新增 {} 条（来源 {}）", added, sourceUrl);
        } catch (Exception e) {
            // 网络/解析失败：静默回退（表内模拟数据继续展示，不空页）
            log.warn("[反诈预警] 真实爬取失败，回退模拟数据：{}", e.getMessage());
        }
    }

    /**
     * 仅保留自动生成记录（mock/real）的最新 KEEP_MAX 条，清理最旧自动记录。
     * 人工维护的预警（source 不在 AUTO_SOURCES 内，如种子数据"青启e城·模拟反诈预警"）永不参与清理，
     * 避免定时任务把人工展示数据当垃圾挤出（曾出现种子数据被误删）。
     */
    private void trimToMax() {
        Long auto = alertMapper.selectCount(new LambdaQueryWrapper<BizAntiFraudAlert>()
                .in(BizAntiFraudAlert::getSource, AUTO_SOURCES));
        if (auto == null || auto <= KEEP_MAX) return;
        List<BizAntiFraudAlert> keep = alertMapper.selectList(
                new LambdaQueryWrapper<BizAntiFraudAlert>()
                        .in(BizAntiFraudAlert::getSource, AUTO_SOURCES)
                        .orderByDesc(BizAntiFraudAlert::getPublishTime)
                        .last("LIMIT " + KEEP_MAX));
        if (!keep.isEmpty()) {
            LocalDateTime oldestKeep = keep.get(keep.size() - 1).getPublishTime();
            alertMapper.delete(new LambdaQueryWrapper<BizAntiFraudAlert>()
                    .in(BizAntiFraudAlert::getSource, AUTO_SOURCES)
                    .lt(BizAntiFraudAlert::getPublishTime, oldestKeep));
            log.info("[反诈预警] 自动数据已超上限，清理至最新 {} 条（人工预警不受影响）", KEEP_MAX);
        }
    }
}
