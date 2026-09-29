package com.icbc.qingqi.module.loan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 借款前 AI 审查日志（模拟AI）
 */
@Data
@TableName("biz_ai_review_log")
public class BizAiReviewLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 申请人ID */
    private Long userId;

    /** 申请人姓名（冗余，便于管理端展示） */
    private String userName;

    /** 授信类型：A_TYPE循环贷 / B_TYPE定向贷 */
    private String creditType;

    /** 业务场景：WITHDRAW提款 / ENTRUST_PAY打款 */
    private String bizType;

    /** 审查结果：PASS通过 / REJECT拒绝 */
    private String result;

    /** AI审查评分（100分制） */
    private Integer aiScore;

    /** 通过项明细（JSON数组） */
    private String passedItems;

    /** 风险项明细（JSON数组） */
    private String rejectedItems;

    /** 关联业务单号（提款流水号/支付单号） */
    private String requestNo;

    /** 审查时间 */
    private LocalDateTime createTime;
}
