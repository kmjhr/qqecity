package com.icbc.qingqi.module.cashflow.dto;

import lombok.Data;

import java.util.List;

/**
 * 防刷单检测结果
 */
@Data
public class AntiBrushResultVO {

    /** 用户 ID */
    private Long userId;

    /** 是否命中风险 */
    private Boolean hit;

    /** 风险分（0-100，越高越危险） */
    private Integer riskScore;

    /** 风险等级：LOW/MEDIUM/HIGH/CRITICAL */
    private String riskLevel;

    /** 命中规则列表 */
    private List<RuleHit> ruleHits;

    /** 跨维比对结果 */
    private CrossCheckResult crossCheck;

    /** 处置建议 */
    private List<String> advices;

    /** 是否模拟 */
    private Boolean simulated;

    @Data
    public static class RuleHit {
        /** 防线：1=异常特征 2=跨维比对 3=平台直取 */
        private Integer defenseLine;
        /** 规则名称 */
        private String ruleName;
        /** 命中描述 */
        private String hitDesc;
        /** 单项扣分 */
        private Integer deductedScore;
        /** 命中样本（脱敏） */
        private List<String> samples;
    }

    @Data
    public static class CrossCheckResult {
        /** 物料采购总额（来自受托支付，模拟） */
        private String materialPurchaseTotal;
        /** 摊位活动记录数（来自记账，模拟） */
        private Integer stallActivityCount;
        /** 流水总收入（来自聚合，模拟） */
        private String aggregatedIncome;
        /** 比对结论 */
        private String conclusion;
        /** 比对偏差率（%） */
        private Integer deviationPercent;
    }
}
