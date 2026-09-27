package com.icbc.qingqi.module.operation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * 个转企条件自查请求 DTO
 * <p>
 * 用户对 6 项自查条件回答 yes/no，给出建议
 */
@Data
public class IndividualToCompanyChecklistDTO {

    /**
     * 自查答案，key=条件编号（1-6），value=true/false
     * 1. 年营业收入是否超过 120 万元（增值税一般纳税人门槛）
     * 2. 是否需要对外开具增值税专用发票
     * 3. 经营风险是否需要有限责任公司隔离个人财产
     * 4. 是否计划引入股权融资或合伙人
     * 5. 是否需要做税务筹划（如企业所得税优惠）
     * 6. 是否有合作伙伴/招标项目要求企业主体
     */
    @NotNull(message = "自查答案不能为空")
    private Map<Integer, Boolean> answers;
}
