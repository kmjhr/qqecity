package com.icbc.qingqi.module.risk.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 风险预警实体
 * <p>
 * 对应表：biz_risk_warning
 * 预警类型：OVERDUE_RISK/HIGH_FREQ_BORROW/CREDIT_ABNORMAL/BUDGET_OVER/CASHFLOW_WARNING
 */
@Data
@TableName("biz_risk_warning")
public class BizRiskWarning {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 预警类型 */
    private String warningType;

    /** 预警等级：LOW/MEDIUM/HIGH/CRITICAL */
    private String warningLevel;

    private String warningTitle;

    private String warningContent;

    /** 关联模块 */
    private String relatedModule;

    /** 关联业务 ID */
    private Long relatedId;

    /** 是否已读 */
    private Integer isRead;

    /** 是否已处理 */
    private Integer isHandled;

    private String handleNote;

    private LocalDateTime warningTime;

    private LocalDateTime handleTime;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
