package com.icbc.qingqi.module.chat.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 对话历史 Redis 存储（TTL 24h）
 * <p>
 * Key: chat:history:{userId}
 * Value: JSON 数组，交替 [user, assistant, user, assistant, ...]
 * 存储格式 "内容@@@MODE"（user 固定 MODE=USER，assistant 为实际引擎 local/agent/fallback）
 * 仅保留最近 20 条（10 轮对话）
 */
@Slf4j
@Component
public class ChatHistoryStore {

    private static final int MAX_MESSAGES = 20;
    private static final String META_SEP = "@@@";

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${chat.history-ttl-hours:24}")
    private int ttlHours;

    public ChatHistoryStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** 历史条目：内容 + 实际引擎模式（user 消息模式为 null） */
    public record HistoryEntry(String content, String mode) {}

    /**
     * 读取历史（返回交替 user/assistant 字符串列表，剥离模式后缀；供 LLM 上下文使用）
     */
    public List<String> loadHistory(Long userId) {
        List<HistoryEntry> entries = loadHistoryEntries(userId);
        List<String> out = new ArrayList<>(entries.size());
        for (HistoryEntry e : entries) {
            out.add(e.content());
        }
        return out;
    }

    /**
     * 读取历史（含模式；供 history 接口恢复真实引擎标签）
     */
    public List<HistoryEntry> loadHistoryEntries(Long userId) {
        try {
            String key = key(userId);
            String json = redis.opsForValue().get(key);
            if (json == null || json.isBlank()) return Collections.emptyList();
            List<String> list = mapper.readValue(json, new TypeReference<List<String>>() {});
            if (list == null) return Collections.emptyList();
            List<HistoryEntry> entries = new ArrayList<>(list.size());
            for (int i = 0; i < list.size(); i++) {
                String raw = list.get(i);
                int idx = raw.lastIndexOf(META_SEP);
                if (idx > 0) {
                    String content = raw.substring(0, idx);
                    String mode = raw.substring(idx + META_SEP.length());
                    entries.add(new HistoryEntry(content, i % 2 == 0 ? null : mode));
                } else {
                    // 兼容旧数据（无模式后缀）
                    entries.add(new HistoryEntry(raw, i % 2 == 0 ? null : "local"));
                }
            }
            return entries;
        } catch (Exception e) {
            log.warn("[ChatHistory] 读取历史失败 userId={}：{}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 追加 user + assistant 消息（assistantMsg 带实际引擎模式），超长截断
     */
    public void appendAndTrim(Long userId, String userMsg, String assistantMsg, String engineMode) {
        try {
            String key = key(userId);
            List<String> list = new ArrayList<>(loadRawList(key));
            list.add(userMsg + META_SEP + "USER");
            list.add(assistantMsg + META_SEP + (engineMode == null ? "local" : engineMode));
            while (list.size() > MAX_MESSAGES) {
                list.remove(0);
                list.remove(0);
            }
            String json = mapper.writeValueAsString(list);
            redis.opsForValue().set(key, json, Duration.ofHours(ttlHours));
        } catch (Exception e) {
            log.warn("[ChatHistory] 写入失败 userId={}：{}", userId, e.getMessage());
        }
    }

    public void clear(Long userId) {
        try {
            redis.delete(key(userId));
        } catch (Exception e) {
            log.warn("[ChatHistory] 清空失败 userId={}：{}", userId, e.getMessage());
        }
    }

    private List<String> loadRawList(String key) {
        try {
            String json = redis.opsForValue().get(key);
            if (json == null || json.isBlank()) return new ArrayList<>();
            List<String> list = mapper.readValue(json, new TypeReference<List<String>>() {});
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            log.warn("[ChatHistory] 读取原始列表失败 key={}：{}", key, e.getMessage());
            return new ArrayList<>();
        }
    }

    private String key(Long userId) {
        return "chat:history:" + userId;
    }
}
