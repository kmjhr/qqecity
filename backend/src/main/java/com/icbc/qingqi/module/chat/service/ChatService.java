package com.icbc.qingqi.module.chat.service;

import com.icbc.qingqi.module.chat.dto.ChatEngineStatusVO;
import com.icbc.qingqi.module.chat.dto.ChatMessageVO;
import com.icbc.qingqi.module.chat.dto.ChatSourceVO;
import com.icbc.qingqi.module.chat.dto.ChatRequestDTO;
import com.icbc.qingqi.module.chat.support.AgentLLMClient;
import com.icbc.qingqi.module.chat.support.ChatHistoryStore;
import com.icbc.qingqi.module.chat.support.LocalRAGEngine;
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

    @Value("${chat.engine:local}")
    private String configuredEngine;

    public ChatService(LocalRAGEngine ragEngine, AgentLLMClient llmClient, ChatHistoryStore historyStore) {
        this.ragEngine = ragEngine;
        this.llmClient = llmClient;
        this.historyStore = historyStore;
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
        String activeEngine;
        String answer;
        if (agentOn) {
            // agent 模式：本地 RAG 拼入上下文 + LLM 组织语言
            String context = ragEngine.buildContext(sources);
            List<String> history = historyStore.loadHistory(userId);
            try {
                String llmAnswer = llmClient.chat(context, history, userMsg);
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

        // 3. 写入历史（带实际引擎模式，供 history 接口恢复真实标签）
        historyStore.appendAndTrim(userId, userMsg, answer, activeEngine);

        // 4. 构造返回 VO
        ChatMessageVO vo = new ChatMessageVO();
        vo.setRole("assistant");
        vo.setContent(answer);
        vo.setSimulated(true);
        vo.setEngineMode(activeEngine);
        vo.setSources(sources);
        vo.setTimestamp(LocalDateTime.now());
        return vo;
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
