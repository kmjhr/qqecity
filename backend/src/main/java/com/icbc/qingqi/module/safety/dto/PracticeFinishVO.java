package com.icbc.qingqi.module.safety.dto;

import lombok.Data;

import java.util.List;

/**
 * 演练结算返回（L3，模拟）
 */
@Data
public class PracticeFinishVO {

    /** 结果：SAFE/LURED/TIMEOUT/FINISHED */
    private String result;

    /** 结果中文 */
    private String resultName;

    /** 综合风险分 0-100（越低越安全） */
    private Integer riskScore;

    /** 风险等级：LOW/MEDIUM/HIGH/CRITICAL */
    private String warningLevel;

    /** 判定结论（复盘摘要） */
    private String resultDesc;

    /** 复盘要点列表 */
    private List<String> reviewPoints;

    /** 模拟标识 */
    private Boolean simulated;
}
