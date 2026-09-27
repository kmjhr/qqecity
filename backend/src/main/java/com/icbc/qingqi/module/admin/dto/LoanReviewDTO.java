package com.icbc.qingqi.module.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理端 - 贷款申请审批参数
 * <p>
 * decision: APPROVED - 通过 / REJECTED - 拒绝 / RETURNED - 退回补充资料
 * rejectReason / returnReason：拒绝或退回原因（REJECTED/RETURNED 必填）
 * approveAmount：审批金额（APPROVED 可选，默认等于申请金额）
 */
@Data
public class LoanReviewDTO {

    /** 审批结论：APPROVED / REJECTED / RETURNED */
    @NotBlank(message = "审批结论不能为空")
    private String decision;

    /** 拒绝原因（REJECTED 时必填） */
    private String rejectReason;

    /** 退回原因（RETURNED 时必填） */
    private String returnReason;

    /** 审批金额（APPROVED 时可选，默认等于 applyAmount） */
    private java.math.BigDecimal approveAmount;

    /** 审批备注 */
    private String remark;
}
