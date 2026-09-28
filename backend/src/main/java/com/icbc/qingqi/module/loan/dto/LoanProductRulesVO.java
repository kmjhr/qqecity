package com.icbc.qingqi.module.loan.dto;

import lombok.Data;

import java.util.List;

/**
 * 青创e贷 A/B 双轨产品规则（模块2 产品介绍）
 * <p>
 * 供前端"产品介绍"页完整展示产品要素、准入规则、资金流向、专属风险提示；
 * 全部为演示模拟口径（银行能力模拟）。
 */
@Data
public class LoanProductRulesVO {

    /** A类产品规则：创业信用画像循环贷 */
    private ProductRule productA;

    /** B类产品规则：小额定向两步式受托支付贷 */
    private ProductRule productB;

    /** 通用风险揭示（与银行信贷产品一致的风险提示） */
    private List<String> generalRisks;

    @Data
    public static class ProductRule {
        /** 产品名称 */
        private String name;
        /** 一句话定位 */
        private String slogan;
        /** 适用对象 */
        private String target;
        /** 额度说明 */
        private String limitDesc;
        /** 年化利率说明 */
        private String rateDesc;
        /** 期限说明 */
        private String termDesc;
        /** 计息方式 */
        private String interestDesc;
        /** 还款方式 */
        private String repayDesc;
        /** 准入规则 */
        private String accessDesc;
        /** 资金流向 */
        private String fundFlowDesc;
        /** 详细规则列表 */
        private List<String> rules;
        /** 专属风险提示 */
        private List<String> risks;
    }
}
