package com.icbc.qingqi.module.safety.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 骗局甄别记录实体
 * <p>
 * 对应表：biz_fraud_detection_log
 * 检测结果：SAFE/SUSPICIOUS/DANGEROUS
 */
@Data
@TableName("biz_fraud_detection_log")
public class BizFraudDetectionLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String inputText;

    /** 检测结果：SAFE/SUSPICIOUS/DANGEROUS */
    private String detectResult;

    /** 风险等级：1-5 */
    private Integer riskLevel;

    /** 命中规则（JSON 数组） */
    private String matchedRules;

    private String warningContent;

    private LocalDateTime detectTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
