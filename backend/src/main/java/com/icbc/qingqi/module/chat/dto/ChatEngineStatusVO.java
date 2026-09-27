package com.icbc.qingqi.module.chat.dto;

import lombok.Data;

/**
 * 对话引擎状态（前端用于显示"AI 增强未开启"提示）
 */
@Data
public class ChatEngineStatusVO {

    /** 当前生效引擎：local / agent */
    private String activeEngine;

    /** 配置引擎：local / agent */
    private String configuredEngine;

    /** agent 是否可用（API Key 是否已配置） */
    private Boolean agentAvailable;

    /** 提示语 */
    private String notice;
}
