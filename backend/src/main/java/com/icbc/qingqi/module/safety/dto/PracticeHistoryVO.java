package com.icbc.qingqi.module.safety.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 演练历史分页返回（L3，模拟）
 */
@Data
public class PracticeHistoryVO {

    private Long total;

    private List<Item> list;

    @Data
    public static class Item {
        private String practiceNo;
        private String scenarioTitle;
        private String result;
        private String resultDesc;
        private Integer riskScore;
        private Integer roundCount;
        private LocalDateTime startedAt;
        private LocalDateTime endedAt;
    }
}
