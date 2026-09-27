package com.icbc.qingqi.module.safety.dto;

import lombok.Data;

import java.util.List;

/**
 * 征信健康管理 VO（模拟）
 * <p>
 * 缺口 #26 征信健康管理（简化版）
 * - 健康分（复用 biz_credit_report.credit_score）
 * - 改善清单：还清逾期 / 减少查询 / 规范还款
 * - 模拟修复路径：演示"一键模拟"展示分阶段变化，标注不产生真实征信影响
 */
@Data
public class CreditHealthVO {

    /** 用户 ID */
    private Long userId;

    /** 健康分（350-850） */
    private Integer healthScore;

    /** 健康等级：EXCELLENT/GOOD/FAIR/POOR */
    private String healthLevel;

    /** 健康等级中文名 */
    private String healthLevelName;

    /** 各维度得分（0-100） */
    private List<DimensionScore> dimensions;

    /** 改善清单 */
    private List<ImprovementItem> improvements;

    /** 模拟标注 */
    private String simulationNotice;

    @Data
    public static class DimensionScore {
        /** 维度 key：REPAYMENT / UTILIZATION / QUERY / OVERDUE */
        private String key;
        /** 维度名称 */
        private String label;
        /** 当前得分 */
        private Integer score;
        /** 评价 */
        private String evaluation;
    }

    @Data
    public static class ImprovementItem {
        /** 改善项 key */
        private String key;
        /** 改善项名称 */
        private String label;
        /** 当前值 */
        private String currentValue;
        /** 目标值 */
        private String targetValue;
        /** 预计提升分数 */
        private Integer estimatedScoreGain;
        /** 操作说明 */
        private String action;
        /** 是否已完成 */
        private Boolean completed;
    }
}
