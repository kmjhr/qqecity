package com.icbc.qingqi.module.safety.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 演练回放返回（L3，模拟）
 */
@Data
public class PracticeDetailVO {

    private String practiceNo;
    private String scenarioTitle;
    private String result;
    private Integer riskScore;
    private String resultDesc;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    /** 回合明细（含诈骗方开场白与用户发言） */
    private List<RoundItem> rounds;

    @Data
    public static class RoundItem {
        private Integer roundNo;
        private String speaker;
        private String content;
        private Integer safeScore;
        private String hitWords;
    }
}
