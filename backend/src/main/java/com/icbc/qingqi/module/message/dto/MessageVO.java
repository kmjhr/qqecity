package com.icbc.qingqi.module.message.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息 VO（返回给前端）
 */
@Data
public class MessageVO {

    /** 消息 ID */
    private Long id;

    /** 消息标题 */
    private String title;

    /** 消息内容 */
    private String content;

    /** 消息类型：SYSTEM-系统通知 / BUDGET-预算提醒 / BUSINESS-业务通知 */
    private String type;

    /** 是否已读：0-未读 / 1-已读 */
    private Integer isRead;

    /** 创建时间 */
    private LocalDateTime createTime;
}
