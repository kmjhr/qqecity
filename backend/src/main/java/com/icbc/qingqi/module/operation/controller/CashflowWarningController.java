package com.icbc.qingqi.module.operation.controller;

import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.module.operation.dto.CashflowWarningVO;
import com.icbc.qingqi.module.operation.service.CashflowWarningService;
import com.icbc.qingqi.module.risk.entity.BizRiskWarning;
import com.icbc.qingqi.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 现金流风险预警
 * <p>
 * 路径：/api/v1/operation/cashflow-warning/**
 * 缺口 #12 现金流风险预警
 * 规则：近3月结余率<10% 且 存在>7天应收未收 → CASHFLOW_WARNING
 */
@Tag(name = "现金流风险预警")
@RestController
@RequestMapping("/v1/operation/cashflow-warning")
public class CashflowWarningController {

    private final CashflowWarningService cashflowWarningService;

    public CashflowWarningController(CashflowWarningService cashflowWarningService) {
        this.cashflowWarningService = cashflowWarningService;
    }

    @Operation(summary = "检测现金流风险并落库预警",
            description = "基于近3月记账数据判定：结余率<10% 且 存在>7天应收未收 → 落 biz_risk_warning(CASHFLOW_WARNING)。7天内同用户不重复触发。")
    @PostMapping("/detect")
    public Result<CashflowWarningVO> detect() {
        Long userId = UserContext.getUserId();
        return Result.success(cashflowWarningService.detect(userId));
    }

    @Operation(summary = "查询历史现金流预警记录")
    @GetMapping("/history")
    public Result<List<BizRiskWarning>> history() {
        Long userId = UserContext.getUserId();
        return Result.success(cashflowWarningService.listWarnings(userId));
    }
}
