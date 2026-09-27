package com.icbc.qingqi.module.operation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 保险代销产品实体（模拟）
 * <p>
 * 对应表：biz_insurance_product
 * 险种：PERFORMANCE_BOND-履约保证 / IP_PATENT-专利执行 / IP_INFRINGEMENT-侵权责任 / PROPERTY-财产综合
 * 合规口径：工行仅代销、不承保；演示用，不构成真实投保邀约
 */
@Data
@TableName("biz_insurance_product")
public class BizInsuranceProduct {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 产品编码 */
    private String productCode;

    /** 产品名称 */
    private String productName;

    /** 险种 */
    private String insuranceType;

    /** 经营场景标签：CONTRACT-合同履约/IP-知识产权/PROPERTY-财产/EMPLOYER-雇主 */
    private String scene;

    /** 适用人群标签（逗号分隔） */
    private String targetCrowd;

    /** 保费费率描述（模拟） */
    private String premiumRate;

    /** 保额上限（模拟） */
    private BigDecimal coverageAmount;

    /** 承保机构（模拟） */
    private String insurer;

    /** 产品要素速览 */
    private String productElements;

    /** 投保条件 */
    private String conditions;

    /** 模拟跳转投保入口URL */
    private String applyUrl;

    /** 状态：ACTIVE/INACTIVE */
    private String status;

    private Integer sortOrder;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
