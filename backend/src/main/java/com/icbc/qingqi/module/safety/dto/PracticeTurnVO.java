package com.icbc.qingqi.module.safety.dto;

import lombok.Data;

/**
 * 用户发言回合返回（L3，模拟）
 */
@Data
public class PracticeTurnVO {

    /** AI（诈骗方）接招话术 */
    private String aiReply;

    /** 本回合安全分 0-100 */
    private Integer safeScore;

    /** 风险等级：SAFE/NEUTRAL/DANGEROUS */
    private String riskLevel;

    /** 危险提示（命中危险词时非空） */
    private String dangerHint;

    /** 消息序号（含诈骗方开场白） */
    private Integer roundNo;

    /** 用户已发言次数 */
    private Integer userTurnNo;

    /** 是否演练结束 */
    private Boolean gameOver;

    /** 结束时的结果：SAFE/LURED/TIMEOUT/FINISHED */
    private String result;

    /** 模拟标识 */
    private Boolean simulated;
}
