package com.icbc.qingqi.module.budget.controller;

import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 模块4 - 碎片消费治理（占位骨架）
 * <p>
 * 路径：/api/v1/budget/**
 * 功能：消费预算管理、心愿储蓄、交易分类、消费分析、理财持仓等
 * <p>
 * 当前为占位实现，返回模拟数据，后续迭代逐步接入真实业务逻辑
 */
@Tag(name = "碎片消费治理")
@RestController
@RequestMapping("/v1/budget")
public class BudgetController {

    @Operation(summary = "获取预算概览（模拟数据）")
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        Long userId = UserContext.getUserId();
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("month", "2026-09");
        data.put("totalBudget", new BigDecimal("3000.00"));
        data.put("totalUsed", new BigDecimal("1880.00"));
        data.put("remainRatio", new BigDecimal("37.33"));
        data.put("remindLevel", 1);
        return Result.success(data);
    }

    @Operation(summary = "获取分类预算列表（模拟数据）")
    @GetMapping("/list")
    public Result<Map<String, Object>> list() {
        Map<String, Object> data = new HashMap<>();
        data.put("total", 2);
        data.put("items", java.util.List.of(
                Map.of("id", 1, "category", "餐饮",
                        "budgetAmount", new BigDecimal("1500.00"),
                        "usedAmount", new BigDecimal("780.00"),
                        "remainRatio", new BigDecimal("48.00"),
                        "remindLevel", 1, "status", "进行中"),
                Map.of("id", 2, "category", "购物",
                        "budgetAmount", new BigDecimal("800.00"),
                        "usedAmount", new BigDecimal("700.00"),
                        "remainRatio", new BigDecimal("12.50"),
                        "remindLevel", 2, "status", "进行中")
        ));
        return Result.success(data);
    }

    @Operation(summary = "获取心愿储蓄（模拟数据）")
    @GetMapping("/savings")
    public Result<Map<String, Object>> savings() {
        Map<String, Object> data = new HashMap<>();
        data.put("total", 1);
        data.put("items", java.util.List.of(
                Map.of("id", 1, "goalName", "旅行基金",
                        "goalAmount", new BigDecimal("5000.00"),
                        "currentAmount", new BigDecimal("2300.00"),
                        "progress", new BigDecimal("46.00"),
                        "status", "进行中")
        ));
        return Result.success(data);
    }

    @Operation(summary = "设置预算（模拟）")
    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> req) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", 100L);
        data.put("category", req.get("category"));
        data.put("budgetAmount", req.get("budgetAmount"));
        data.put("status", "success");
        data.put("message", "预算设置成功（模拟）");
        return Result.success(data);
    }
}
