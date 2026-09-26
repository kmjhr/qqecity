package com.icbc.qingqi.module.message;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息提醒（message）
 */
@Data
@TableName("message")
public class Message {

    @TableId(type = IdType.AUTO)
    private Long msgId;
    private Long userId;
    /** 提醒类型：预算提醒/风险预警/政策推送/业务通知 */
    private String msgType;
    private String msgContent;
    /** 推送渠道 */
    private String pushChannel;
    private LocalDateTime sendTime;
    /** 已读状态：0未读/1已读 */
    private Integer readStatus;
}
