package com.icbc.qingqi.module.chat.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Agent LLM 客户端（豆包/DeepSeek 兼容 OpenAI 协议）
 * <p>
 * API Key 安全红线：仅读 ${CHAT_LLM_API_KEY:} 环境变量占位符；
 * Key 为空 → isAvailable()=false → 自动禁用 agent 回退 local；
 * Key 不写入代码、不提交 git、不告知 AI（本类的 API Key 通过 Spring @Value 注入环境变量值）
 */
@Slf4j
@Component
public class AgentLLMClient {

    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${chat.llm.endpoint}")
    private String endpoint;

    @Value("${chat.llm.model}")
    private String model;

    @Value("${chat.llm.timeout-seconds:15}")
    private int timeoutSeconds;

    @Value("${chat.llm.max-tokens:1024}")
    private int maxTokens;

    @Value("${chat.llm.api-key:}")
    private String apiKey;

    @Value("${chat.llm.system-prompt}")
    private String systemPrompt;

    /**
     * 是否可用（API Key 已配置）
     */
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * 调用 LLM：context（本地 RAG 召回拼入上下文）+ history（对话历史）+ userMessage
     * 返回 LLM 生成的回答
     */
    public String chat(String context, List<String> historyMessages, String userMessage) {
        return chat(context, historyMessages, userMessage, systemPrompt);
    }

    /**
     * 调用 LLM（可覆盖 system prompt，供反诈演练等场景使用角色人设而非客服人设）
     */
    public String chat(String context, List<String> historyMessages, String userMessage, String systemOverride) {
        if (!isAvailable()) {
            throw new IllegalStateException("LLM API Key 未配置，agent 模式不可用");
        }
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", model);
            body.put("max_tokens", maxTokens);
            body.put("temperature", 0.3);

            ArrayNode messages = body.putArray("messages");
            // 系统提示词（演练等场景可覆盖为角色人设）
            ObjectNode sysMsg = messages.addObject();
            sysMsg.put("role", "system");
            sysMsg.put("content", systemOverride + "\n\n【知识库上下文】\n" + context);

            // 历史对话
            for (int i = 0; i < historyMessages.size(); i += 2) {
                ObjectNode user = messages.addObject();
                user.put("role", "user");
                user.put("content", historyMessages.get(i));
                if (i + 1 < historyMessages.size()) {
                    ObjectNode asst = messages.addObject();
                    asst.put("role", "assistant");
                    asst.put("content", historyMessages.get(i + 1));
                }
            }
            // 当前问题
            ObjectNode cur = messages.addObject();
            cur.put("role", "user");
            cur.put("content", userMessage);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                    .build();

            HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                throw new RuntimeException("LLM HTTP " + resp.statusCode() + ": " + resp.body());
            }
            JsonNode root = mapper.readTree(resp.body());
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            if (contentNode.isMissingNode()) {
                throw new RuntimeException("LLM 响应缺少 choices[0].message.content");
            }
            return contentNode.asText();
        } catch (Exception e) {
            log.warn("[AgentLLM] 调用失败，将自动降级 local 模式：{}", e.getMessage());
            throw new RuntimeException("LLM 调用失败：" + e.getMessage(), e);
        }
    }
}
