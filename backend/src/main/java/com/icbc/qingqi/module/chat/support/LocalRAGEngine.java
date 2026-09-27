package com.icbc.qingqi.module.chat.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icbc.qingqi.module.chat.dto.ChatSourceVO;
import com.icbc.qingqi.module.policy.entity.BizPolicy;
import com.icbc.qingqi.module.policy.mapper.BizPolicyMapper;
import com.icbc.qingqi.module.safety.entity.BizAntiFraudContent;
import com.icbc.qingqi.module.safety.mapper.BizAntiFraudContentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 本地 RAG 召回引擎（离线可用）
 * <p>
 * 知识源：FAQ 50 条 + biz_policy 政策库 + biz_anti_fraud_content 反诈库
 * 召回方式：关键词分词匹配 + 命中数排序，取 Top N
 * <p>
 * 所有结果带来源引用（可点开溯源），标注"模拟对话引擎/仅供参考"
 */
@Slf4j
@Component
public class LocalRAGEngine {

    private final BizPolicyMapper policyMapper;
    private final BizAntiFraudContentMapper antiFraudMapper;

    @Value("${chat.rag.max-snippets:5}")
    private int maxSnippets;

    @Autowired
    public LocalRAGEngine(BizPolicyMapper policyMapper,
                          BizAntiFraudContentMapper antiFraudMapper) {
        this.policyMapper = policyMapper;
        this.antiFraudMapper = antiFraudMapper;
    }

    /**
     * 本地召回：返回拼好的上下文片段（含来源），并填充 sources 列表
     */
    public List<ChatSourceVO> recall(String message, String scene) {
        String lower = message == null ? "" : message.toLowerCase();
        List<Hit> hits = new ArrayList<>();

        // 1. FAQ 关键词命中
        for (FaqData.FaqItem item : FaqData.FAQ_LIST) {
            int score = 0;
            for (String kw : item.keywords) {
                if (lower.contains(kw)) {
                    score++;
                }
            }
            if (score > 0) {
                hits.add(new Hit(score, item.answer, buildFaqSource(item)));
            }
        }

        // 2. biz_policy 政策库命中（标题/条件/政策类型）
        if (lower.contains("安居") || lower.contains("人才") || lower.contains("公寓")
                || lower.contains("贴息") || lower.contains("创业贷") || lower.contains("政策")
                || lower.contains("补贴") || lower.contains("个转企") || lower.contains("税费")) {
            List<BizPolicy> policies = policyMapper.selectList(
                    new LambdaQueryWrapper<BizPolicy>()
                            .eq(BizPolicy::getStatus, "ACTIVE")
                            .orderByAsc(BizPolicy::getSortOrder));
            for (BizPolicy p : policies) {
                int score = scorePolicy(p, lower);
                if (score > 0) {
                    hits.add(new Hit(score, buildPolicySnippet(p), buildPolicySource(p)));
                }
            }
        }

        // 3. biz_anti_fraud_content 反诈库命中（标题/分类/内容）
        if (lower.contains("诈骗") || lower.contains("刷单") || lower.contains("公检法")
                || lower.contains("征信洗白") || lower.contains("防骗") || lower.contains("骗局")
                || lower.contains("情景") || "FRAUD".equalsIgnoreCase(scene)) {
            List<BizAntiFraudContent> contents = antiFraudMapper.selectList(
                    new LambdaQueryWrapper<BizAntiFraudContent>()
                            .eq(BizAntiFraudContent::getStatus, 1)
                            .orderByAsc(BizAntiFraudContent::getSortOrder));
            for (BizAntiFraudContent c : contents) {
                int score = scoreAntiFraud(c, lower);
                if (score > 0) {
                    hits.add(new Hit(score, buildAntiFraudSnippet(c), buildAntiFraudSource(c)));
                }
            }
        }

        // 4. 按命中数排序取 Top N
        hits.sort(Comparator.comparingInt((Hit h) -> h.score).reversed());
        Set<String> seen = new HashSet<>();
        List<ChatSourceVO> result = new ArrayList<>();
        for (Hit h : hits) {
            if (result.size() >= maxSnippets) break;
            if (seen.contains(h.source.getUrl())) continue;
            seen.add(h.source.getUrl());
            result.add(h.source);
        }
        return result;
    }

    /**
     * 拼接上下文文本（供 LLM 模式使用）
     */
    public String buildContext(List<ChatSourceVO> sources) {
        if (sources == null || sources.isEmpty()) {
            return "（未召回到相关知识，请基于常识回答并标注'模拟对话引擎/仅供参考'）";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("以下是从工行青启e城知识库召回的资料（编号→内容）：\n");
        for (int i = 0; i < sources.size(); i++) {
            ChatSourceVO s = sources.get(i);
            sb.append("[").append(i + 1).append("] ")
                    .append("来源:").append(s.getSourceType())
                    .append(" ID:").append(s.getSourceId())
                    .append(" 标题:").append(s.getTitle()).append("\n")
                    .append("   摘要:").append(s.getSnippet()).append("\n");
        }
        return sb.toString();
    }

    /**
     * local 模式：基于召回片段模板化生成回答
     */
    public String buildLocalAnswer(String message, List<ChatSourceVO> sources) {
        if (sources == null || sources.isEmpty()) {
            return "（模拟对话引擎/仅供参考）\n抱歉，本地知识库未召回到相关内容。"
                    + "可尝试询问保函/贷款/预算/记账/反诈/政策/征信/保险/理财等关键词。";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("（模拟对话引擎/仅供参考）\n");
        sb.append("基于本地知识库为您解答：\n\n");
        for (int i = 0; i < sources.size(); i++) {
            ChatSourceVO s = sources.get(i);
            sb.append(i + 1).append(". ").append(s.getSnippet()).append("\n");
        }
        sb.append("\n来源引用见下方，可点开溯源。");
        return sb.toString();
    }

    // ============================================================
    // 评分与片段构造
    // ============================================================

    private int scorePolicy(BizPolicy p, String lower) {
        int score = 0;
        if (p.getPolicyName() != null && lower.contains(p.getPolicyName().toLowerCase())) score += 3;
        if (p.getPolicyType() != null) {
            if ("HOUSING".equals(p.getPolicyType()) && lower.contains("安居")) score += 2;
            if ("ENTREPRENEUR".equals(p.getPolicyType()) && (lower.contains("贴息") || lower.contains("创业"))) score += 2;
        }
        if (p.getConditions() != null) {
            String c = p.getConditions().toLowerCase();
            for (String kw : new String[]{"学生", "应届", "创业", "毕业", "落户"}) {
                if (lower.contains(kw) && c.contains(kw)) score += 1;
            }
        }
        return score;
    }

    private int scoreAntiFraud(BizAntiFraudContent c, String lower) {
        int score = 0;
        if (c.getTitle() != null && lower.contains(c.getTitle().toLowerCase())) score += 3;
        if (c.getCategory() != null && lower.contains(c.getCategory().toLowerCase())) score += 2;
        if (c.getSummary() != null) {
            String s = c.getSummary().toLowerCase();
            for (String kw : new String[]{"刷单", "公检法", "征信", "洗白", "骗局", "诈骗"}) {
                if (lower.contains(kw) && s.contains(kw)) score += 1;
            }
        }
        return score;
    }

    private String buildPolicySnippet(BizPolicy p) {
        StringBuilder sb = new StringBuilder();
        sb.append("【政策").append(p.getPolicyNo()).append("】").append(p.getPolicyName()).append("。");
        sb.append("类型:").append("HOUSING".equals(p.getPolicyType()) ? "人才安居" : "创业贴息").append("；");
        if (p.getMaxAmount() != null) sb.append("最高额度:").append(p.getMaxAmount()).append("元；");
        if (p.getSubsidyRate() != null) sb.append("贴息/补贴:").append(p.getSubsidyRate()).append("；");
        if (p.getConditions() != null) sb.append("条件:").append(p.getConditions()).append("；");
        return sb.toString();
    }

    private String buildAntiFraudSnippet(BizAntiFraudContent c) {
        StringBuilder sb = new StringBuilder();
        sb.append("【反诈").append(c.getId()).append("】").append(c.getTitle()).append("。");
        if (c.getSummary() != null) sb.append(c.getSummary());
        return sb.toString();
    }

    private ChatSourceVO buildFaqSource(FaqData.FaqItem item) {
        ChatSourceVO vo = new ChatSourceVO();
        vo.setSourceType("FAQ");
        vo.setSourceId(item.id);
        vo.setTitle(item.title);
        vo.setSnippet(truncate(item.answer, 200));
        vo.setUrl(item.url);
        return vo;
    }

    private ChatSourceVO buildPolicySource(BizPolicy p) {
        ChatSourceVO vo = new ChatSourceVO();
        vo.setSourceType("POLICY");
        vo.setSourceId(p.getId());
        vo.setTitle(p.getPolicyName());
        vo.setSnippet(truncate(buildPolicySnippet(p), 200));
        vo.setUrl(p.getApplyUrl() != null ? p.getApplyUrl() : "/pages/policy/match");
        return vo;
    }

    private ChatSourceVO buildAntiFraudSource(BizAntiFraudContent c) {
        ChatSourceVO vo = new ChatSourceVO();
        vo.setSourceType("ANTI_FRAUD");
        vo.setSourceId(c.getId());
        vo.setTitle(c.getTitle());
        vo.setSnippet(truncate(buildAntiFraudSnippet(c), 200));
        vo.setUrl("/pages/safety/fraud/" + c.getId());
        return vo;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }

    private static class Hit {
        final int score;
        final String snippet;
        final ChatSourceVO source;
        Hit(int score, String snippet, ChatSourceVO source) {
            this.score = score;
            this.snippet = snippet;
            this.source = source;
        }
    }
}
