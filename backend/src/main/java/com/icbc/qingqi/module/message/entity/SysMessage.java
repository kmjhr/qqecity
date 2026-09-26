package com.icbc.qingqi.module.message.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息实体
 * <p>
 * 对应表：sys_message
 * 消息类型：
 * - SYSTEM   系统通知
 * - BUDGET   预算提醒
 * - BUSINESS 业务通知
 * <p>
 * 已读状态：
 * - 0 未读
 * - 1 已读
 */
@Data
@TableName("sys_message")
public class SysMessage {

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收用户 ID */
    private Long userId;

    /** 消息标题 */
    private String title;

    /** 消息内容 */
    private String content;

    /**
     * 消息类型
     * SYSTEM-系统通知 / BUDGET-预算提醒 / BUSINESS-业务通知 / SAFETY-安全预警
     */
    private String type;

    /** 业务类型：GUARANTEE-保函 / LOAN-贷款 / BUDGET-预算 等 */
    private String bizType;

    /** 关联业务 ID */
    private Long bizId;

    /** 是否已读：0-未读 / 1-已读 */
    private Integer isRead;

    /** 阅读时间 */
    private LocalDateTime readTime;

    /** 逻辑删除：0-存在，1-删除 */
    @TableLogic
    private Integer deleted;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
