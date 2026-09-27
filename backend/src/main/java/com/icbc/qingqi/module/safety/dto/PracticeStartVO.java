package com.icbc.qingqi.module.safety.dto;

import lombok.Data;

import java.util.List;

/**
 * 开始对话演练返回（L3，模拟）
 */
@Data
public class PracticeStartVO {

    /** 演练编号 */
    private String practiceNo;

    /** 情景标题 */
    private String scenarioTitle;

    /** 情景背景 */
    private String background;

    /** AI（诈骗方）开场白 */
    private String openingLine;

    /** 剧本话术总轮数 */
    private Integer scriptRounds;

    /** 用户可选应对提示（引导自由发言） */
    private List<String> tips;

    /** 模拟标识 */
    private Boolean simulated;
}
