package com.icbc.qingqi.module.guarantee.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 保函申请表单 DTO
 * <p>
 * 对应 G-1：录入房东信息、租赁要素、押金金额
 * 提交后系统自动创建/关联 biz_landlord、biz_house、biz_rental_contract、biz_guarantee_application
 */
@Data
public class GuaranteeApplyDTO {

    // ---------- 房东信息 ----------

    /** 房东姓名 */
    @NotBlank(message = "房东姓名不能为空")
    private String landlordName;

    /** 房东电话（用于查找或创建房东记录） */
    @NotBlank(message = "房东电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "房东手机号格式不正确")
    private String landlordPhone;

    /** 房东身份证号（可选） */
    private String landlordIdCard;

    // ---------- 房屋信息 ----------

    /** 房屋标题 */
    @NotBlank(message = "房屋标题不能为空")
    private String houseTitle;

    /** 省 */
    private String province;

    /** 市 */
    private String city;

    /** 区 */
    private String district;

    /** 详细地址 */
    @NotBlank(message = "房屋详细地址不能为空")
    private String address;

    /** 房屋类型：APARTMENT/HOUSE */
    private String houseType;

    /** 面积（平方米） */
    private BigDecimal area;

    /** 居室数 */
    private Integer roomCount;

    // ---------- 租赁要素 ----------

    /** 月租金 */
    @NotNull(message = "月租金不能为空")
    @DecimalMin(value = "0.01", message = "月租金必须大于 0")
    private BigDecimal monthlyRent;

    /** 押金金额（保函金额） */
    @NotNull(message = "押金金额不能为空")
    @DecimalMin(value = "0.01", message = "押金金额必须大于 0")
    private BigDecimal depositAmount;

    /** 租期开始日 */
    @NotNull(message = "租期开始日不能为空")
    private LocalDate rentStartDate;

    /** 租期结束日 */
    @NotNull(message = "租期结束日不能为空")
    private LocalDate rentEndDate;

    /** 付款方式：MONTHLY-月付 / QUARTERLY-季付 */
    private String payMethod;

    /** 合同条款文本（AI 复审关键词扫描来源，可选） */
    private String contractTerms;
}
