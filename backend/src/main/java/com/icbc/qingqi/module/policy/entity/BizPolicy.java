package com.icbc.qingqi.module.policy.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 政策库实体
 * <p>
 * 对应表：biz_policy
 * 人才安居政策 + 创业贴息政策，按人群标签匹配推送
 */
@Data
@TableName("biz_policy")
public class BizPolicy {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 政策编号 */
    private String policyNo;

    /** 政策名称 */
    private String policyName;

    /** 政策类型：HOUSING-安居 / ENTREPRENEUR-创业贴息 */
    private String policyType;

    /**
     * 适用人群标签（逗号分隔）
     * STUDENT-在校生 / GRADUATE-毕业2年内 / ENTREPRENEUR-青年创业者 / OTHER-其他青年
     */
    private String targetCrowd;

    /** 最高补贴/贷款额度 */
    private BigDecimal maxAmount;

    /** 贴息比例/补贴标准 */
    private String subsidyRate;

    /** 申报条件速览 */
    private String conditions;

    /** 申报入口URL（真实官方申报链接） */
    private String applyUrl;

    /** 政策生效日期 */
    private java.time.LocalDate validFrom;

    /** 政策失效日期（NULL=长期有效） */
    private java.time.LocalDate validTo;

    /** 匹配关键词（逗号分隔） */
    private String keywordTags;

    /** 地区（全国/省/直辖市） */
    private String region;

    /** 政策概要（卡片速览） */
    private String policySummary;

    /** 政策来源 */
    private String policySource;

    /** 状态：ACTIVE/INACTIVE */
    private String status;

    /** 排序 */
    private Integer sortOrder;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
