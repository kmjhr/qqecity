package com.icbc.qingqi.module.safety.dto;

import lombok.Data;

import java.util.List;

/**
 * 征信健康模拟修复路径 VO（模拟）
 * <p>
 * 演示"一键模拟修复路径"，展示分阶段变化与预计信用分提升
 * <p>
 * 重要：不产生真实征信影响，仅用于演示与教育
 */
@Data
public class SimulateFixVO {

    /** 用户 ID */
    private Long userId;

    /** 起始信用分（修复前） */
    private Integer startScore;

    /** 预期信用分（修复后，6 个月） */
    private Integer endScore;

    /** 预期提升分数 */
    private Integer scoreGain;

    /** 分阶段修复路径 */
    private List<FixStage> stages;

    /** 模拟标注 */
    private String simulationNotice;

    @Data
    public static class FixStage {
        /** 阶段：M1/M3/M6（第1/3/6月） */
        private String stage;
        /** 阶段名称 */
        private String stageName;
        /** 操作动作 */
        private String action;
        /** 预期信用分 */
        private Integer expectedScore;
        /** 累计提升 */
        private Integer gainFromStart;
        /** 说明 */
        private String explanation;
    }
}
