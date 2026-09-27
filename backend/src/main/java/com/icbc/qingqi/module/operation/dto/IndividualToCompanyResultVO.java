package com.icbc.qingqi.module.operation.dto;

import lombok.Data;

import java.util.List;

/**
 * 个转企引导自查结果 VO
 */
@Data
public class IndividualToCompanyResultVO {

    /** 命中条件数 */
    private Integer matchedCount;

    /** 总条件数 */
    private Integer totalCount;

    /** 建议结论：RECOMMEND-建议转企 / OPTIONAL-可选 / NOT_RECOMMEND-暂不建议 */
    private String conclusion;

    /** 结论说明 */
    private String conclusionDesc;

    /** 命中的条件说明 */
    private List<String> matchedReasons;

    /** 未命中条件说明 */
    private List<String> unmatchedReasons;

    /** 后续引导步骤 */
    private List<String> nextSteps;

    /** 模拟声明 */
    private String complianceNotice;
}
