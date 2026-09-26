package com.icbc.qingqi.module.bookkeeping.controller;

import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 模块3 - 创业经营赋能（占位骨架）
 * <p>
 * 路径：/api/v1/bookkeeping/**
 * 功能：AI智能记账、经营流水分析、利润测算、政策推送、保险代销等
 * <p>
 * 当前为占位实现，返回模拟数据，后续迭代逐步接入真实业务逻辑
 */
@Tag(name = "创业经营赋能")
@RestController
@RequestMapping("/v1/bookkeeping")
public class BookkeepingController {

    @Operation(summary = "获取经营概览（模拟数据）")
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        Long userId = UserContext.getUserId();
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("monthRevenue", new BigDecimal("8500.00"));
        data.put("monthExpense", new BigDecimal("3200.00"));
        data.put("monthProfit", new BigDecimal("5300.00"));
        data.put("profitGrowth", "12.5%");
        data.put("totalOrders", 42);
        return Result.success(data);
    }

    @Operation(summary = "获取记账列表（模拟数据）")
    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Map<String, Object> data = new HashMap<>();
        data.put("total", 20);
        data.put("pageNum", pageNum);
        data.put("pageSize", pageSize);
        data.put("items", java.util.List.of(
                Map.of("id", 1, "type", "收入", "category", "商品销售",
                        "amount", new BigDecimal("580.00"), "date", "2026-09-25",
                        "description", "文创产品销售"),
                Map.of("id", 2, "type", "支出", "category", "物料采购",
                        "amount", new BigDecimal("200.00"), "date", "2026-09-24",
                        "description", "原材料采购"),
                Map.of("id", 3, "type", "收入", "category", "服务收入",
                        "amount", new BigDecimal("300.00"), "date", "2026-09-23",
                        "description", "设计服务收入")
        ));
        return Result.success(data);
    }

    @Operation(summary = "获取政策推荐（模拟数据）")
    @GetMapping("/policies")
    public Result<Map<String, Object>> policies() {
        Map<String, Object> data = new HashMap<>();
        data.put("total", 2);
        data.put("items", java.util.List.of(
                Map.of("id", 1, "policyName", "创业担保贷款（演示）",
                        "policyType", "创业担保贷款", "amountLimit", "最高30万元",
                        "applyUrl", "https://example.com/policy/1"),
                Map.of("id", 2, "policyName", "财政贴息新政（演示）",
                        "policyType", "财政贴息", "amountLimit", "以当年财政公告为准",
                        "applyUrl", "https://example.com/policy/3")
        ));
        return Result.success(data);
    }
}
