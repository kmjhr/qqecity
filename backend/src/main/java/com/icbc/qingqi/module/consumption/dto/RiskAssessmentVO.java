package com.icbc.qingqi.module.consumption.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 风险测评结果 VO
 */
@Data
public class RiskAssessmentVO {

    private Long id;
    private Long userId;
    private String assessNo;

    /** 答案详情：[{q:1,opt:"A",score:2},...] */
    private List<AnswerDetail> answerDetails;

    private Integer totalScore;

    /** 风险等级：CONSERVATIVE/STEADY/BALANCED */
    private String riskLevel;

    private String riskLevelName;

    /** 等级说明 */
    private String riskLevelDesc;

    private LocalDate validUntil;

    /** 是否过期 */
    private Boolean expired;

    /** 是否最新 */
    private Integer isLatest;

    private LocalDateTime assessTime;

    @Data
    public static class AnswerDetail {
        private Integer questionId;
        private String optionCode;
        private Integer score;
        private String questionTitle;
        private String optionLabel;
    }
}
