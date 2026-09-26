package com.icbc.qingqi.module.loan;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 受托支付记录（entrusted_payment）
 */
@Data
@TableName("entrusted_payment")
public class EntrustedPayment {

    @TableId(type = IdType.AUTO)
    private Long paymentId;
    /** 贷款申请ID */
    private Long loanApplyId;
    /** 收款商户白名单ID */
    private Long merchantId;
    private String merchantName;
    private BigDecimal payAmount;
    private LocalDateTime payTime;
    /** 支付状态：0处理中/1成功/2失败 */
    private Integer payStatus;
    private String voucherUrl;
}
