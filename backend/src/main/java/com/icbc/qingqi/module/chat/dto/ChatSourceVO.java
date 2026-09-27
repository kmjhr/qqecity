package com.icbc.qingqi.module.chat.dto;

import lombok.Data;

/**
 * 对话回答来源引用（可点开溯源）
 */
@Data
public class ChatSourceVO {

    /** 来源类型：FAQ / POLICY / ANTI_FRAUD / LLM */
    private String sourceType;

    /** 来源 ID（FAQ 序号 / biz_policy.id / biz_anti_fraud_content.id） */
    private Long sourceId;

    /** 来源标题 */
    private String title;

    /** 来源摘要（截断 200 字） */
    private String snippet;

    /** 跳转链接（模拟，可点开溯源） */
    private String url;
}
