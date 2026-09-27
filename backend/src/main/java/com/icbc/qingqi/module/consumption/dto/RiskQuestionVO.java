package com.icbc.qingqi.module.consumption.dto;

import lombok.Data;

import java.util.List;

/**
 * 风险测评题目 VO
 */
@Data
public class RiskQuestionVO {

    /** 题号 1-10 */
    private Integer id;

    /** 题干 */
    private String title;

    /** 选项列表 */
    private List<Option> options;

    @Data
    public static class Option {
        /** 选项标识：A/B/C/D */
        private String code;

        /** 选项内容 */
        private String label;

        /** 选项分值（1-4） */
        private Integer score;
    }
}
