package com.icbc.qingqi.module.risk.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.module.risk.dto.RiskOverviewVO;
import com.icbc.qingqi.module.risk.entity.BizRiskWarning;
import com.icbc.qingqi.module.risk.mapper.BizRiskWarningMapper;
import com.icbc.qingqi.security.UserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 风险预警聚合接口（用户端，模拟）
 * <p>
 * 聚合 5 类风险预警（逾期风险/高频借贷/征信异常/预算超支/现金流预警）：
 * 各业务模块在命中规则时写入共享表 biz_risk_warning（warning_type 区分），
 * 本接口按当前登录用户一次返回全量分类列表 + 未处理数。
 */
@RestController
@RequestMapping("/v1/risk")
public class RiskController {

    private final BizRiskWarningMapper riskWarningMapper;

    public RiskController(BizRiskWarningMapper riskWarningMapper) {
        this.riskWarningMapper = riskWarningMapper;
    }

    /**
     * 风险预警总览：5 类分类列表 + 未处理数 + 总数
     */
    @GetMapping("/overview")
    public Result<RiskOverviewVO> overview() {
        Long userId = UserContext.getUserId();
        List<BizRiskWarning> all = riskWarningMapper.selectList(
                new LambdaQueryWrapper<BizRiskWarning>()
                        .eq(BizRiskWarning::getUserId, userId)
                        .orderByDesc(BizRiskWarning::getWarningTime));

        RiskOverviewVO vo = new RiskOverviewVO();
        int unhandled = 0;
        for (BizRiskWarning w : all) {
            if (w.getIsHandled() == null || w.getIsHandled() == 0) {
                unhandled++;
            }
            switch (w.getWarningType()) {
                case "OVERDUE_RISK" -> vo.getOverdue().add(w);
                case "HIGH_FREQ_BORROW" -> vo.getHighFreqBorrow().add(w);
                case "CREDIT_ABNORMAL" -> vo.getCreditAbnormal().add(w);
                case "BUDGET_OVER" -> vo.getBudgetOver().add(w);
                case "CASHFLOW_WARNING" -> vo.getCashflow().add(w);
                default -> { /* 未知类型忽略 */ }
            }
        }
        vo.setUnhandledCount(unhandled);
        vo.setTotalCount(all.size());
        return Result.success(vo);
    }
}
