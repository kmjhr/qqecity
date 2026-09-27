package com.icbc.qingqi.module.profile.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 青年成长信用画像
 * <p>
 * 三场景聚合（安居/创业/消费）→ 三维评分（稳定性/经营力/资金健康度）+ 成长轨迹 + 联动演示
 */
@Data
public class ProfileVO {

    /** 用户 ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 人群类型：STUDENT/GRADUATE/ENTREPRENEUR/OTHER */
    private String userType;

    /** 三维评分 */
    private ScoreDetail stability;        // 稳定性（安居）
    private ScoreDetail operation;        // 经营力（创业）
    private ScoreDetail fundHealth;       // 资金健康度（消费）

    /** 综合评分（三维加权平均） */
    private Integer overallScore;

    /** 成长轨迹（按时间倒序） */
    private List<GrowthTrack> growthTrack;

    /** 联动演示：安居稳+经营好→授信提额 */
    private LinkageDemo linkage;

    /** 是否模拟 */
    private Boolean simulated;

    @Data
    public static class ScoreDetail {
        /** 场景：HOUSING/ENTREPRENEUR/CONSUMPTION */
        private String scene;
        /** 维度名 */
        private String dimensionName;
        /** 分数 0-100 */
        private Integer score;
        /** 评级：LOW/MEDIUM/HIGH */
        private String level;
        /** 评分依据 */
        private List<String> evidences;
        /** 数据快照（关键指标） */
        private List<DataPoint> dataPoints;
    }

    @Data
    public static class DataPoint {
        private String label;
        private String value;
    }

    @Data
    public static class GrowthTrack {
        private String period;       // 2026-09
        private String event;
        private String scene;        // HOUSING/ENTREPRENEUR/CONSUMPTION
        private Integer scoreDelta;
    }

    @Data
    public static class LinkageDemo {
        /** 是否符合联动条件 */
        private Boolean eligible;
        /** 提额前额度 */
        private BigDecimal beforeLimit;
        /** 提额后额度 */
        private BigDecimal afterLimit;
        /** 提额幅度 */
        private BigDecimal upliftAmount;
        /** 利率优惠前 */
        private BigDecimal beforeRate;
        /** 利率优惠后 */
        private BigDecimal afterRate;
        /** 联动说明 */
        private String explanation;
        /** 是否模拟 */
        private Boolean simulated;
    }
}
