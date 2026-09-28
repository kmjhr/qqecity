package com.icbc.qingqi.module.loan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户提交自定义商户（B类受托支付收款方）
 * 提交后进入 PENDING 灰名单，由管理端 banker 审核通过（VERIFIED）后方可用于受托支付
 */
@Data
@Schema(description = "自定义商户申请")
public class MerchantApplyDTO {

    @Schema(description = "商户名称")
    @NotBlank(message = "商户名称不能为空")
    private String merchantName;

    @Schema(description = "商户类型：MATERIAL-物料/STALL-摊位/PROMOTION-推广/OTHER-其他")
    private String merchantType;

    @Schema(description = "联系人")
    private String contactName;

    @Schema(description = "联系电话")
    @NotBlank(message = "联系电话不能为空")
    private String contactPhone;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "营业执照号")
    private String businessLicense;

    @Schema(description = "收款账号")
    @NotBlank(message = "收款账号不能为空")
    private String bankAccount;

    @Schema(description = "开户银行")
    @NotBlank(message = "开户银行不能为空")
    private String bankName;

    @Schema(description = "申请说明")
    private String applyRemark;
}
