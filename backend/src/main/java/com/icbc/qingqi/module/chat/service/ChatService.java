package com.icbc.qingqi.module.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icbc.qingqi.module.chat.dto.ChatEngineStatusVO;
import com.icbc.qingqi.module.chat.dto.ChatMessageVO;
import com.icbc.qingqi.module.chat.dto.ChatSourceVO;
import com.icbc.qingqi.module.chat.dto.ChatRequestDTO;
import com.icbc.qingqi.module.chat.support.AgentLLMClient;
import com.icbc.qingqi.module.chat.support.ChatHistoryStore;
import com.icbc.qingqi.module.chat.support.LocalRAGEngine;
import com.icbc.qingqi.module.chat.support.RuleCalculator;
import com.icbc.qingqi.module.consumption.entity.BizCreditReport;
import com.icbc.qingqi.module.consumption.entity.BizRiskAssessment;
import com.icbc.qingqi.module.consumption.mapper.BizCreditReportMapper;
import com.icbc.qingqi.module.consumption.mapper.BizRiskAssessmentMapper;
import com.icbc.qingqi.module.user.entity.SysUser;
import com.icbc.qingqi.module.user.mapper.SysUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话服务（双模式：local / agent）
 * <p>
 * local：本地 RAG 召回 + 模板化回答（离线可用）
 * agent：本地 RAG 召回拼入上下文 + LLM 组织语言 + 来源仍附本地召回（LLM 失败自动降级 local）
 * <p>
 * 全程标注"模拟对话引擎/仅供参考"
 */
@Slf4j
@Service
public class ChatService {

    private final LocalRAGEngine ragEngine;
    private final AgentLLMClient llmClient;
    private final ChatHistoryStore historyStore;
    private final RuleCalculator ruleCalculator;
    private final SysUserMapper sysUserMapper;
    private final BizRiskAssessmentMapper riskAssessmentMapper;
    private final BizCreditReportMapper creditReportMapper;

    @Value("${chat.engine:local}")
    private String configuredEngine;

    public ChatService(LocalRAGEngine ragEngine, AgentLLMClient llmClient, ChatHistoryStore historyStore,
                       RuleCalculator ruleCalculator, SysUserMapper sysUserMapper,
                       BizRiskAssessmentMapper riskAssessmentMapper, BizCreditReportMapper creditReportMapper) {
        this.ragEngine = ragEngine;
        this.llmClient = llmClient;
        this.historyStore = historyStore;
        this.ruleCalculator = ruleCalculator;
        this.sysUserMapper = sysUserMapper;
        this.riskAssessmentMapper = riskAssessmentMapper;
        this.creditReportMapper = creditReportMapper;
    }

    /**
     * 引擎状态（前端用于显示"AI 增强未开启"）
     */
    public ChatEngineStatusVO getEngineStatus() {
        ChatEngineStatusVO vo = new ChatEngineStatusVO();
        vo.setConfiguredEngine(configuredEngine);
        boolean agentOn = "agent".equalsIgnoreCase(configuredEngine) && llmClient.isAvailable();
        vo.setAgentAvailable(agentOn);
        vo.setActiveEngine(agentOn ? "agent" : "local");
        if ("agent".equalsIgnoreCase(configuredEngine) && !llmClient.isAvailable()) {
            vo.setNotice("AI 增强未开启（未配置 CHAT_LLM_API_KEY），已自动回退 local 模式；"
                    + "请在项目根 .env 配置后重启服务（演示用，所有银行能力模拟）");
        } else if (agentOn) {
            vo.setNotice("AI 增强已开启（agent 模式，LLM 仅组织语言，回答仍附本地来源）。模拟对话引擎/仅供参考");
        } else {
            vo.setNotice("当前 local 模式（离线可用，FAQ 50 条 + 政策库 + 反诈库召回）。模拟对话引擎/仅供参考");
        }
        return vo;
    }

    /**
     * 处理用户消息
     */
    public ChatMessageVO handle(ChatRequestDTO dto, Long userId) {
        String userMsg = dto.getMessage();
        String scene = dto.getScene() == null ? "GENERAL" : dto.getScene().toUpperCase();

        // 1. 本地 RAG 召回（local 与 agent 都先召回）
        List<ChatSourceVO> sources = ragEngine.recall(userMsg, scene);

        // 2. 决策引擎模式：请求级 mode 优先（local/agent），缺省用服务端全局配置 chat.engine
        String effectiveEngine = configuredEngine;
        if (dto.getMode() != null && !dto.getMode().isBlank()) {
            String m = dto.getMode().trim().toLowerCase();
            if ("local".equals(m) || "agent".equals(m)) {
                effectiveEngine = m;
            }
        }
        boolean agentOn = "agent".equalsIgnoreCase(effectiveEngine) && llmClient.isAvailable();

        // ④ 个性化画像（agent 模式拼入上下文，供 LLM 结合画像给出建议）
        String profile = buildProfile(userId);

        String activeEngine;
        String answer;
        if (agentOn) {
            // agent 模式：本地 RAG 拼入上下文 + 用户画像 + LLM 组织语言
            String context = ragEngine.buildContext(sources) + profile;
            List<String> history = historyStore.loadHistory(userId);
            try {
                String llmAnswer = llmClient.chat(context, history, userMsg);
                // 兜底清理：无论 LLM 在何处输出"（模拟对话引擎/仅供参考）"标注（行首/行尾/段尾），全部剥离，由系统统一前缀一次
                llmAnswer = llmAnswer
                        .replace("模拟对话引擎/仅供参考", "")
                        .replace("来源引用见下方，可点开溯源。", "")
                        .replaceAll("[（(][)）]", "")
                        .replaceAll("\\n{3,}", "\n\n")
                        .trim();
                activeEngine = "agent";
                answer = "（模拟对话引擎/仅供参考）\n" + llmAnswer;
            } catch (Exception e) {
                log.warn("[Chat] agent 调用失败，自动降级 local：{}", e.getMessage());
                activeEngine = "fallback";
                answer = ragEngine.buildLocalAnswer(userMsg, sources);
            }
        } else {
            // local 模式：模板化回答
            activeEngine = "local";
            answer = ragEngine.buildLocalAnswer(userMsg, sources);
        }

        // ① 规则推理计算（本地/agent 均附）：识别押金/保函金额并计算保函费；LLM 若已复述计算模板则不重复追加
        String calc = ruleCalculator.guaranteeFee(userMsg);
        if (calc != null && !answer.contains("【计算（模拟）】")) {
            answer = answer + "\n" + calc;
        }

        // 3. 写入历史（带实际引擎模式，供 history 接口恢复真实标签）
        historyStore.appendAndTrim(userId, userMsg, answer, activeEngine);

        // 4. 构造返回 VO
        ChatMessageVO vo = new ChatMessageVO();
        vo.setRole("assistant");
        vo.setContent(answer);
        vo.setSimulated(true);
        vo.setEngineMode(activeEngine);
        vo.setSources(sources);
        vo.setAction(detectAction(userMsg));
        vo.setTimestamp(LocalDateTime.now());
        return vo;
    }

    /**
     * ③ 意图识别 → 功能跳转动作（关键词规则，返回可点跳转的功能页）
     */
    private ChatMessageVO.ChatActionVO detectAction(String msg) {
        if (msg == null || msg.isBlank()) return null;
        String m = msg.toLowerCase();
        if (m.contains("保函") || m.contains("押金") || m.contains("租房") || m.contains("租金")) {
            return action("去申请保函", "/pages/guarantee/apply");
        }
        if (m.contains("贷款") || m.contains("青创") || m.contains("创业贷") || m.contains("额度")) {
            return action("去申请青创e贷", "/pages/loan/apply");
        }
        if (m.contains("预算") || m.contains("记账") || m.contains("账单")) {
            return action("去记账", "/pages/budget");
        }
        if (m.contains("诈骗") || m.contains("反诈") || m.contains("刷单") || m.contains("骗")) {
            return action("去反诈专区", "/pages/safety");
        }
        if (m.contains("政策") || m.contains("补贴") || m.contains("贴息") || m.contains("安居")) {
            return action("去政策匹配", "/pages/policy/match");
        }
        if (m.contains("征信") || m.contains("信用")) {
            return action("查看信用健康", "/pages/credit");
        }
        if (m.contains("理财") || m.contains("基金")) {
            return action("去低风险理财", "/pages/wealth");
        }
        if (m.contains("保险")) {
            return action("去看保险", "/pages/insurance");
        }
        return null;
    }

    private ChatMessageVO.ChatActionVO action(String label, String url) {
        ChatMessageVO.ChatActionVO a = new ChatMessageVO.ChatActionVO();
        a.setLabel(label);
        a.setUrl(url);
        return a;
    }

    /**
     * ④ 用户画像（演示参考）：用户类型/学校 + 最新风险测评 + 最新信用报告等级
     */
    private String buildProfile(Long userId) {
        try {
            StringBuilder sb = new StringBuilder();
            SysUser u = sysUserMapper.selectById(userId);
            if (u != null) {
                if (u.getUserType() != null) sb.append("用户类型:").append(userTypeName(u.getUserType())).append("；");
                if (u.getSchool() != null && !u.getSchool().isBlank()) sb.append("学校:").append(u.getSchool()).append("；");
            }
            BizRiskAssessment ra = riskAssessmentMapper.selectOne(
                    new LambdaQueryWrapper<BizRiskAssessment>()
                            .eq(BizRiskAssessment::getUserId, userId)
                            .eq(BizRiskAssessment::getIsLatest, 1)
                            .eq(BizRiskAssessment::getDeleted, 0)
                            .last("LIMIT 1"));
            if (ra != null && ra.getRiskLevelName() != null) {
                sb.append("风险测评:").append(ra.getRiskLevelName()).append("；");
            }
            BizCreditReport cr = creditReportMapper.selectOne(
                    new LambdaQueryWrapper<BizCreditReport>()
                            .eq(BizCreditReport::getUserId, userId)
                            .eq(BizCreditReport::getDeleted, 0)
                            .orderByDesc(BizCreditReport::getQueryTime)
                            .last("LIMIT 1"));
            if (cr != null && cr.getCreditLevel() != null) {
                sb.append("信用等级:").append(cr.getCreditLevel()).append("；");
            }
            if (sb.length() == 0) return "";
            return "\n【用户画像（仅演示参考）】" + sb
                    + "\n（回答时可结合画像给出个性化建议，但不得编造画像中没有的数据）";
        } catch (Exception e) {
            log.warn("[Chat] 用户画像读取失败 userId={}：{}", userId, e.getMessage());
            return "";
        }
    }

    private String userTypeName(String t) {
        if (t == null) return null;
        switch (t.toUpperCase()) {
            case "STUDENT": return "大学生";
            case "ENTREPRENEUR": return "创业青年";
            case "NEW_CITIZEN": return "新市民青年";
            default: return t;
        }
    }

    /**
     * 查询历史（按 user/assistant 交替返回，assistant 恢复真实引擎模式）
     */
    public List<ChatMessageVO> history(Long userId) {
        List<ChatHistoryStore.HistoryEntry> entries = historyStore.loadHistoryEntries(userId);
        List<ChatMessageVO> result = new java.util.ArrayList<>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            ChatHistoryStore.HistoryEntry e = entries.get(i);
            ChatMessageVO m = new ChatMessageVO();
            m.setRole(i % 2 == 0 ? "user" : "assistant");
            m.setContent(e.content());
            m.setSimulated(true);
            m.setEngineMode(i % 2 == 0 ? null : e.mode());
            result.add(m);
        }
        return result;
    }

    /**
     * 清空历史
     */
    public void clear(Long userId) {
        historyStore.clear(userId);
    }
}
