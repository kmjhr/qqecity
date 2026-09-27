package com.icbc.qingqi.module.consumption.controller;

import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.module.consumption.service.ConsumeGuideService;
import com.icbc.qingqi.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 三层渐进式消费引导
 * <p>
 * 路径：/api/v1/consumption/guide/**
 * 缺口 #18 三层消费引导（简化）
 */
@Tag(name = "三层渐进式消费引导")
@RestController
@RequestMapping("/v1/consumption/guide")
public class ConsumeGuideController {

    private final ConsumeGuideService service;

    public ConsumeGuideController(ConsumeGuideService service) {
        this.service = service;
    }

    // ============================================================
    //  第一层：交易后即时推送
    // ============================================================

    @Operation(summary = "① 交易后即时推送（模拟交易触发）",
            description = "检测指定交易是否大额（> 预算月度 30%），命中发站内信。")
    @PostMapping("/after-txn/{transactionId}")
    public Result<ConsumeGuideService.AfterTxnResult> afterTxn(@PathVariable Long transactionId) {
        Long userId = UserContext.getUserId();
        return Result.success(service.afterTransactionPush(userId, transactionId));
    }

    // ============================================================
    //  第二层：月度账单分析
    // ============================================================

    @Operation(summary = "② 月度账单分析（消费结构诊断 + 负债预警）",
            description = "period 不传则默认当月（yyyy-MM 格式）")
    @GetMapping("/monthly-bill")
    public Result<ConsumeGuideService.MonthlyBillAnalysis> monthlyBill(
            @Parameter(description = "账单月份，如 2026-09") @RequestParam(required = false) String period) {
        Long userId = UserContext.getUserId();
        return Result.success(service.analyzeMonthlyBill(userId, period));
    }

    // ============================================================
    //  第三层：支付前实时提醒演示页
    // ============================================================

    @Operation(summary = "③ 支付前实时提醒（演示页）",
            description = "返回提醒文案 + 预算检查结果。标注「仅工行自有支付场景」。")
    @GetMapping("/pay-before-reminder")
    public Result<ConsumeGuideService.PayBeforeReminder> payBeforeReminder(
            @Parameter(description = "支付金额") @RequestParam BigDecimal amount,
            @Parameter(description = "商户名称") @RequestParam(required = false) String merchantName) {
        Long userId = UserContext.getUserId();
        return Result.success(service.payBeforeReminder(userId, amount, merchantName));
    }
}
