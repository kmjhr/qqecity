package com.icbc.qingqi.module.policy.sync;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icbc.qingqi.module.policy.entity.BizPolicy;
import com.icbc.qingqi.module.policy.mapper.BizPolicyMapper;
import com.icbc.qingqi.module.policy.sync.dto.PolicySyncResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 政策定时同步任务（方案B：定时抓取官方源）
 * <p>
 * - 按 policy.sync.cron 定时执行（默认每天 02:00），可通过 policy.sync.enabled 关闭
 * - 遍历配置的官方栏目源，抓取并解析政策条目，按申报链接去重后入库
 * - 单源失败自动跳过并记录错误，不影响其他源与现有数据
 * - 管理端可手动触发 POST /api/v1/admin/policy/sync（ADMIN/BANK_OPERATOR）
 */
@Slf4j
@Service
public class PolicySyncTask {

    private final PolicySyncProperties properties;
    private final PolicySourceAdapter adapter;
    private final BizPolicyMapper policyMapper;

    public PolicySyncTask(PolicySyncProperties properties,
                          PolicySourceAdapter adapter,
                          BizPolicyMapper policyMapper) {
        this.properties = properties;
        this.adapter = adapter;
        this.policyMapper = policyMapper;
    }

    private static final List<String> TAG_KEYWORDS = List.of(
            "毕业生", "在校生", "大学生", "创业", "社保", "贷款", "补贴", "住房", "租房",
            "就业", "无房", "免费", "人才", "优惠", "税费", "见习", "灵活就业");

    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*万");

    /** 定时同步（每天 02:00，可配置） */
    @Scheduled(cron = "${policy.sync.cron:0 0 2 * * ?}")
    public void scheduledSync() {
        if (!properties.isEnabled()) {
            log.info("[政策同步] 定时同步已关闭（policy.sync.enabled=false），跳过");
            return;
        }
        log.info("[政策同步] 定时同步开始，共 {} 个官方源", properties.getSources().size());
        List<PolicySyncResult> results = syncNow();
        int added = results.stream().mapToInt(PolicySyncResult::getAdded).sum();
        log.info("[政策同步] 定时同步完成：新增 {} 条", added);
    }

    /** 立即同步全部源（供定时任务与管理端接口共用） */
    public List<PolicySyncResult> syncNow() {
        List<PolicySyncResult> results = new ArrayList<>();
        int maxSort = getMaxSortOrder();
        for (PolicySyncProperties.Source source : properties.getSources()) {
            PolicySyncResult result;
            try {
                List<PolicySourceItem> items = adapter.fetch(source.getUrl());
                int added = 0;
                int skipped = 0;
                int index = 0;
                for (PolicySourceItem item : items) {
                    if (existsByUrl(item.getUrl())) {
                        skipped++;
                        continue;
                    }
                    policyMapper.insert(toPolicy(source, item, maxSort + added + index));
                    added++;
                    index++;
                }
                result = new PolicySyncResult(source.getName(), source.getRegion(), added, skipped, true, null);
                log.info("[政策同步] 源 {}（{}）新增 {} / 跳过 {}",
                        source.getName(), source.getRegion(), added, skipped);
            } catch (Exception e) {
                result = new PolicySyncResult(source.getName(), source.getRegion(), 0, 0, false, e.getMessage());
                log.warn("[政策同步] 源 {}（{}）抓取失败，已跳过：{}",
                        source.getName(), source.getRegion(), e.getMessage());
            }
            results.add(result);
        }
        return results;
    }

    private int getMaxSortOrder() {
        List<BizPolicy> list = policyMapper.selectList(new LambdaQueryWrapper<BizPolicy>()
                .orderByDesc(BizPolicy::getSortOrder).last("limit 1"));
        return list.isEmpty() || list.get(0).getSortOrder() == null ? 0 : list.get(0).getSortOrder();
    }

    private boolean existsByUrl(String url) {
        return policyMapper.selectCount(new LambdaQueryWrapper<BizPolicy>()
                .eq(BizPolicy::getApplyUrl, url)) > 0;
    }

    private BizPolicy toPolicy(PolicySyncProperties.Source source, PolicySourceItem item, int sort) {
        BizPolicy p = new BizPolicy();
        p.setPolicyNo("SYNC-" + source.getName().toUpperCase() + "-" + sha1(item.getUrl()).substring(0, 8));
        p.setPolicyName(item.getTitle().length() > 60 ? item.getTitle().substring(0, 60) : item.getTitle());
        p.setPolicyType(detectType(item.getTitle()));
        p.setTargetCrowd(detectCrowd(item.getTitle()));
        p.setMaxAmount(detectAmount(item.getTitle()));
        p.setSubsidyRate(item.getTitle());
        p.setConditions("定时同步自官方栏目页自动采集，申报条件详见官方申报页面（模拟）。");
        p.setApplyUrl(item.getUrl());
        p.setValidFrom(LocalDate.now());
        p.setValidTo(null);
        p.setKeywordTags(detectTags(item.getTitle()));
        p.setRegion(source.getRegion());
        p.setPolicySummary(item.getTitle());
        p.setPolicySource("定时同步采集（" + source.getName() + "）");
        p.setStatus("ACTIVE");
        p.setSortOrder(sort);
        return p;
    }

    private String detectType(String title) {
        for (String k : List.of("住房", "租房", "安居", "安家", "人才公寓", "保障性", "驿站", "公租房")) {
            if (title.contains(k)) {
                return "HOUSING";
            }
        }
        return "ENTREPRENEUR";
    }

    private String detectCrowd(String title) {
        Set<String> c = new LinkedHashSet<>();
        if (title.contains("毕业生") || title.contains("离校")) c.add("GRADUATE");
        if (title.contains("在校生") || title.contains("大学生") || title.contains("高校")) c.add("STUDENT");
        if (title.contains("创业") || title.contains("经营者")) c.add("ENTREPRENEUR");
        if (c.isEmpty()) c.add("ENTREPRENEUR");
        c.add("GRADUATE");
        return String.join(",", c);
    }

    private BigDecimal detectAmount(String title) {
        Matcher m = AMOUNT_PATTERN.matcher(title);
        if (m.find()) {
            try {
                return new BigDecimal(m.group(1)).multiply(BigDecimal.valueOf(10000));
            } catch (Exception ignore) {
            }
        }
        return BigDecimal.ZERO;
    }

    private String detectTags(String title) {
        Set<String> tags = new LinkedHashSet<>();
        for (String k : TAG_KEYWORDS) {
            if (title.contains(k)) {
                tags.add(k);
            }
        }
        if (tags.isEmpty()) {
            tags.add("青年");
        }
        return String.join(",", tags);
    }

    private String sha1(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(s.hashCode());
        }
    }
}
