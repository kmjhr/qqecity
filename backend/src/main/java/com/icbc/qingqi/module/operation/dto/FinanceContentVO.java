package com.icbc.qingqi.module.operation.dto;

import lombok.Data;

import java.util.List;

/**
 * 财税科普内容 VO
 * <p>
 * 内容分类：TAX_POPULARIZATION-税务优惠 / INVOICE_GUIDE-发票常识
 */
@Data
public class FinanceContentVO {

    private Long id;

    /** 内容分类：TAX_POPULARIZATION-税务优惠 / INVOICE_GUIDE-发票常识 */
    private String category;

    private String categoryName;

    private String title;

    private String summary;

    /** 正文（按段落数组返回，便于前端排版） */
    private List<String> content;

    /** 关键要点（数组） */
    private List<String> keyPoints;

    /** 模拟声明 */
    private String complianceNotice;
}
