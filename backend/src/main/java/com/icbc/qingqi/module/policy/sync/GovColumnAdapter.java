package com.icbc.qingqi.module.policy.sync;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 政府静态栏目页适配器
 * <p>
 * 适配各级人社/房管网站的静态栏目列表页（col/post 结构，如 hrss.gd.gov.cn / col 页）：
 * 抓取页面 → 提取 <a> 链接 → 按政策关键词过滤标题 → 返回政策条目。
 * 容错：抓取/解析失败抛异常，由 PolicySyncTask 逐源捕获，不影响其他源与现有数据。
 */
@Slf4j
@Component
public class GovColumnAdapter implements PolicySourceAdapter {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36";

    /** 政策标题必须命中以下任一关键词 */
    private static final List<String> POLICY_KEYWORDS = List.of(
            "补贴", "创业", "就业", "毕业生", "人才", "贷款", "政策", "优惠", "扶持", "贴息", "保障");

    /** 导航/功能性链接排除词 */
    private static final List<String> EXCLUDE_KEYWORDS = List.of(
            "首页", "登录", "注册", "联系我们", "网站地图", "无障碍", "设为首页", "收藏本站",
            "部门简介", "机构设置", "信息公开指南", "年报", "预算", "决算", "政务公开目录");

    @Override
    public List<PolicySourceItem> fetch(String url) throws IOException {
        Document doc = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(15000)
                .followRedirects(true)
                .get();
        Set<PolicySourceItem> items = new LinkedHashSet<>();
        for (Element a : doc.select("a[href]")) {
            String title = a.text().trim().replaceAll("\\s+", " ");
            String href = a.absUrl("href");
            if (!isPolicyTitle(title)) {
                continue;
            }
            if (!href.startsWith("http://") && !href.startsWith("https://")) {
                continue;
            }
            items.add(new PolicySourceItem(title, href));
        }
        List<PolicySourceItem> list = items.stream()
                .filter(i -> i.getTitle().length() <= 80)
                .collect(Collectors.toList());
        log.info("[政策同步] 源 {} 解析出 {} 条政策条目", url, list.size());
        return list;
    }

    private boolean isPolicyTitle(String title) {
        if (title == null || title.length() < 6 || title.length() > 80) {
            return false;
        }
        for (String k : POLICY_KEYWORDS) {
            if (title.contains(k)) {
                for (String e : EXCLUDE_KEYWORDS) {
                    if (title.contains(e)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }
}
