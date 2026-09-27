package com.icbc.qingqi.module.safety.controller;

import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.module.risk.entity.BizRiskWarning;
import com.icbc.qingqi.module.safety.dto.OverdueRiskVO;
import com.icbc.qingqi.module.safety.service.OverdueRiskService;
import com.icbc.qingqi.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 逾期风险预判（模拟）
 * <p>
 * 路径：/api/v1/safety/overdue-risk/**
 * 缺口 #15 逾期风险预判
 * 基于还款日历（贷款/受托支付/保函费）+ 收支规则评分，命中落 OVERDUE_RISK
 */
@Tag(name = "逾期风险预判（模拟）")
@RestController
@RequestMapping("/v1/safety/overdue-risk")
public class OverdueRiskController {

    private final OverdueRiskService service;

    public OverdueRiskController(OverdueRiskService service) {
        this.service = service;
    }

    @Operation(summary = "查询还款日历（未来 N 天）",
            description = "聚合 A 类循环贷、受托支付、保函费的应还清单；返回账户余额与未来净现金流（模拟）。")
    @GetMapping("/calendar")
    public Result<OverdueRiskVO> calendar(@RequestParam(defaultValue = "30") Integer days) {
        Long userId = UserContext.getUserId();
        return Result.success(service.getCalendar(userId, days));
    }

    @Operation(summary = "执行逾期风险预判",
            description = "aheadDays=7（默认）。规则：余额不足应还 / 近30天净现金流为负 / 多笔到期覆盖率<60% → OVERDUE_RISK。7 天去重。")
    @PostMapping("/predict")
    public Result<OverdueRiskVO> predict(@RequestParam(defaultValue = "7") Integer aheadDays) {
        Long userId = UserContext.getUserId();
        return Result.success(service.predict(userId, aheadDays));
    }

    @Operation(summary = "查询历史逾期风险预警")
    @GetMapping("/warnings")
    public Result<List<BizRiskWarning>> warnings() {
        Long userId = UserContext.getUserId();
        return Result.success(service.listWarnings(userId));
    }
}
