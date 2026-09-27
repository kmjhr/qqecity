package com.icbc.qingqi.module.consumption.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 风险测评记录实体
 * <p>
 * 对应表：biz_risk_assessment
 * 风险等级：CONSERVATIVE-保守 / STEADY-稳健 / BALANCED-平衡
 * 有效期：1 年，过期需重新测评
 */
@Data
@TableName("biz_risk_assessment")
public class BizRiskAssessment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String assessNo;

    /** 答案 JSON：[{q:1,opt:"A",score:2},...] */
    private String answers;

    private Integer totalScore;

    /** 风险等级：CONSERVATIVE/STEADY/BALANCED */
    private String riskLevel;

    private String riskLevelName;

    private LocalDate validUntil;

    /** 是否最新：1最新 0历史 */
    private Integer isLatest;

    private LocalDateTime assessTime;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
