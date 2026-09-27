package com.icbc.qingqi.module.consumption.controller;

import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.module.consumption.entity.BizCreditReport;
import com.icbc.qingqi.module.consumption.service.CreditMonitorService;
import com.icbc.qingqi.module.risk.entity.BizRiskWarning;
import com.icbc.qingqi.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 常态化征信监测（模拟）
 * <p>
 * 路径：/api/v1/consumption/credit-monitor/**
 * 缺口 #14 常态化征信监测
 * 软查询模拟（不产生硬查询），异常转 CREDIT_ABNORMAL 预警
 */
@Tag(name = "常态化征信监测（模拟）")
@RestController
@RequestMapping("/v1/consumption/credit-monitor")
public class CreditMonitorController {

    private final CreditMonitorService service;

    public CreditMonitorController(CreditMonitorService service) {
        this.service = service;
    }

    @Operation(summary = "授权并软查询模拟征信报告",
            description = "用户授权后软查询模拟（biz_credit_report.source=SIMULATED），不产生硬查询；异常借贷/逾期自动转 CREDIT_ABNORMAL 预警。")
    @PostMapping("/soft-query")
    public Result<BizCreditReport> softQuery() {
        Long userId = UserContext.getUserId();
        return Result.success(service.softQuery(userId));
    }

    @Operation(summary = "查询最新一份模拟征信报告")
    @GetMapping("/latest")
    public Result<BizCreditReport> latest() {
        Long userId = UserContext.getUserId();
        return Result.success(service.latestReport(userId));
    }

    @Operation(summary = "查询全部历史征信报告")
    @GetMapping("/reports")
    public Result<List<BizCreditReport>> listReports() {
        Long userId = UserContext.getUserId();
        return Result.success(service.listReports(userId));
    }

    @Operation(summary = "查询历史征信异常预警")
    @GetMapping("/warnings")
    public Result<List<BizRiskWarning>> warnings() {
        Long userId = UserContext.getUserId();
        return Result.success(service.listWarnings(userId));
    }
}
