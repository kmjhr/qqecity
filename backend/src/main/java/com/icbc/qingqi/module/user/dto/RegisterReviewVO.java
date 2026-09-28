package com.icbc.qingqi.module.user.dto;

import lombok.Data;

import java.util.List;

/**
 * 注册 AI 审核结果 VO（模拟）
 * <p>
 * 预审接口 /auth/register/ai-review 与正式注册 /auth/register 共用
 */
@Data
public class RegisterReviewVO {

    /** 审核编号（REG+时间戳；预审时生成但未落库） */
    private String reviewNo;

    /** 是否全部通过：true-可注册，false-拒绝注册 */
    private Boolean passed;

    /** 逐项审核明细 */
    private List<RegisterReviewItemVO> items;

    /** 拒绝原因（passed=true 时为空） */
    private String rejectReason;

    /** 注册成功后的用户ID（仅正式注册返回） */
    private Long userId;

    /** 模拟标注：AI 审核为演示规则引擎 */
    private Boolean simulated = Boolean.TRUE;
}
