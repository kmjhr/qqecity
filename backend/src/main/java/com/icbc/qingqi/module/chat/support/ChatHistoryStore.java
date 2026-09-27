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
 * 仅保留最近 20 条（10 轮对话）
 */
@Slf4j
@Component
public class ChatHistoryStore {

    private static final int MAX_MESSAGES = 20;

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${chat.history-ttl-hours:24}")
    private int ttlHours;

    public ChatHistoryStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * 读取历史（返回交替 user/assistant 字符串列表）
     */
    public List<String> loadHistory(Long userId) {
        try {
            String key = key(userId);
            String json = redis.opsForValue().get(key);
            if (json == null || json.isBlank()) return Collections.emptyList();
            List<String> list = mapper.readValue(json, new TypeReference<List<String>>() {});
            return list != null ? list : Collections.emptyList();
        } catch (Exception e) {
            log.warn("[ChatHistory] 读取历史失败 userId={}：{}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 追加 user + assistant 消息，超长截断
     */
    public void appendAndTrim(Long userId, String userMsg, String assistantMsg) {
        try {
            String key = key(userId);
            List<String> list = new ArrayList<>(loadHistory(userId));
            list.add(userMsg);
            list.add(assistantMsg);
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

    private String key(Long userId) {
        return "chat:history:" + userId;
    }
}
