package com.icbc.qingqi.module.chat.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话单条消息 VO（user / assistant）
 */
@Data
public class ChatMessageVO {

    /** 角色：user / assistant */
    private String role;

    /** 文本内容 */
    private String content;

    /** 是否模拟（始终 true） */
    private Boolean simulated;

    /** 引擎模式：local / agent / fallback（agent 失败回退） */
    private String engineMode;

    /** 来源引用（仅 assistant 返回） */
    private List<ChatSourceVO> sources;

    private LocalDateTime timestamp;
}
