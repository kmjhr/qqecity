package com.icbc.qingqi.module.safety.dto;

import lombok.Data;

import java.util.List;

/**
 * 反诈情景化教学 VO
 * <p>
 * 缺口 #1 反诈情景化教学
 * 基于 biz_anti_fraud_content（content_type=SCENARIO_SIM）扩展
 * 互动问答：刷单诈骗/冒充公检法/征信洗白
 * 选择 → 反馈 → 解析，选错有纠偏文案
 */
@Data
public class ScenarioVO {

    /** 内容 ID */
    private Long id;

    /** 标题 */
    private String title;

    /** 分类：刷单诈骗/冒充公检法/征信洗白 等 */
    private String category;

    /** 摘要 */
    private String summary;

    /** 情景背景描述 */
    private String background;

    /** 问题列表 */
    private List<Question> questions;

    /** 模拟标注 */
    private String simulationNotice;

    @Data
    public static class Question {
        /** 题号（从1开始） */
        private Integer questionNo;
        /** 题干 */
        private String stem;
        /** 选项 */
        private List<Option> options;
        /** 题目解析（提交后返回） */
        private String explanation;
        /** 用户选择（提交后返回） */
        private String userChoice;
        /** 是否答对（提交后返回） */
        private Boolean correct;
        /** 纠偏文案（答错时返回） */
        private String correction;
    }

    @Data
    public static class Option {
        /** 选项标识：A/B/C/D */
        private String key;
        /** 选项内容 */
        private String text;
    }
}
