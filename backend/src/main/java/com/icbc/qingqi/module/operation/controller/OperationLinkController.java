package com.icbc.qingqi.module.operation.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.module.loan.entity.BizCreditLimit;
import com.icbc.qingqi.module.loan.mapper.BizCreditLimitMapper;
import com.icbc.qingqi.module.risk.entity.BizRiskWarning;
import com.icbc.qingqi.module.risk.mapper.BizRiskWarningMapper;
import com.icbc.qingqi.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 创业经营×消费治理×贷款 联动预警聚合
 * <p>
 * 路径：/api/v1/operation/loan-linked-warnings
 * 聚合模块3/4 产生的风险预警（现金流/预算超支/高频借贷等），结合在贷余额输出对贷款画像与额度的联动影响提示。
 */
@Tag(name = "模块3/4×贷款联动预警")
@RestController
@RequestMapping("/v1/operation/loan-linked")
public class OperationLinkController {

    private final BizRiskWarningMapper riskWarningMapper;
    private final BizCreditLimitMapper creditLimitMapper;

    public OperationLinkController(BizRiskWarningMapper riskWarningMapper,
                                   BizCreditLimitMapper creditLimitMapper) {
        this.riskWarningMapper = riskWarningMapper;
        this.creditLimitMapper = creditLimitMapper;
    }

    @Operation(summary = "模块3/4 风险预警 × 贷款联动",
            description = "返回本人未处理预警（现金流/预算超支/高频借贷/征信监测）+ 在贷余额 + 联动影响提示（模拟）。")
    @GetMapping("/warnings")
    public Result<Map<String, Object>> linkedWarnings() {
        Long userId = UserContext.getUserId();

        List<BizRiskWarning> warnings = riskWarningMapper.selectList(
                new LambdaQueryWrapper<BizRiskWarning>()
                        .eq(BizRiskWarning::getUserId, userId)
                        .eq(BizRiskWarning::getIsHandled, 0)
                        .in(BizRiskWarning::getRelatedModule, List.of("BUDGET", "BOOKKEEPING", "CREDIT_REPORT", "LOAN"))
                        .orderByDesc(BizRiskWarning::getWarningTime));

        // 在贷余额（A+B 已用额度）
        BigDecimal outstanding = BigDecimal.ZERO;
        List<BizCreditLimit> limits = creditLimitMapper.selectList(
                new LambdaQueryWrapper<BizCreditLimit>()
                        .eq(BizCreditLimit::getUserId, userId)
                        .eq(BizCreditLimit::getStatus, "ACTIVE"));
        for (BizCreditLimit l : limits) {
            if (l.getUsedLimit() != null) outstanding = outstanding.add(l.getUsedLimit());
        }

        // 联动影响：有未处理预警且在贷>0 → 提示降额/冻结风险
        String impact;
        if (!warnings.isEmpty() && outstanding.compareTo(BigDecimal.ZERO) > 0) {
            impact = "存在 " + warnings.size() + " 条经营/消费风险预警，将拉低负债纪律与经营力评分，可能触发额度降额或冻结（模拟），建议及时处理或提前还款。";
        } else if (!warnings.isEmpty()) {
            impact = "存在 " + warnings.size() + " 条经营/消费风险预警，建议尽快处理（模拟）。";
        } else {
            impact = "当前无联动风险预警，画像与额度状态良好。";
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (BizRiskWarning w : warnings) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", w.getId());
            m.put("warningType", w.getWarningType());
            m.put("warningLevel", w.getWarningLevel());
            m.put("warningTitle", w.getWarningTitle());
            m.put("warningContent", w.getWarningContent());
            m.put("relatedModule", w.getRelatedModule());
            m.put("warningTime", w.getWarningTime());
            items.add(m);
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("outstandingAmount", outstanding.setScale(2, BigDecimal.ROUND_HALF_UP));
        map.put("warningCount", warnings.size());
        map.put("impact", impact);
        map.put("warnings", items);
        return Result.success(map);
    }
}
