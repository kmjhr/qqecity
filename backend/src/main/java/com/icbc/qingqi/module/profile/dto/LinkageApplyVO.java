package com.icbc.qingqi.module.profile.dto;

import lombok.Data;

/**
 * 联动提额应用结果
 */
@Data
public class LinkageApplyVO {

    /** 是否应用成功 */
    private Boolean applied;

    /** 提额后总额度 */
    private java.math.BigDecimal totalLimit;

    /** 提额后可用额度 */
    private java.math.BigDecimal availableLimit;

    /** 优惠后利率 */
    private java.math.BigDecimal interestRate;

    /** 提示语 */
    private String notice;

    /** 是否模拟 */
    private Boolean simulated;
}
