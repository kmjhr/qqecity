package com.icbc.qingqi.module.loan.controller;

import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 模块2 - 轻创业智能授信（占位骨架）
 * <p>
 * 路径：/api/v1/loan/**
 * 功能：青创e贷申请、智能授信预审、额度查询、受托支付等
 * <p>
 * 当前为占位实现，返回模拟数据，后续迭代逐步接入真实业务逻辑
 */
@Tag(name = "轻创业智能授信")
@RestController
@RequestMapping("/v1/loan")
public class LoanController {

    @Operation(summary = "获取授信额度（模拟数据）")
    @GetMapping("/credit")
    public Result<Map<String, Object>> credit() {
        Long userId = UserContext.getUserId();
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("creditType", "A");
        data.put("creditAmount", new BigDecimal("50000.00"));
        data.put("usedAmount", new BigDecimal("12000.00"));
        data.put("remainAmount", new BigDecimal("38000.00"));
        data.put("validStart", LocalDate.of(2026, 9, 1).toString());
        data.put("validEnd", LocalDate.of(2027, 9, 1).toString());
        data.put("creditStatus", "正常");
        return Result.success(data);
    }

    @Operation(summary = "贷款申请（模拟）")
    @PostMapping("/apply")
    public Result<Map<String, Object>> apply(@RequestBody Map<String, Object> req) {
        Map<String, Object> data = new HashMap<>();
        data.put("applyId", 2001L);
        data.put("loanType", req.getOrDefault("loanType", "B"));
        data.put("applyAmount", req.getOrDefault("applyAmount", new BigDecimal("15000.00")));
        data.put("precheckStatus", "PASS");
        data.put("precheckRange", "10000-20000");
        data.put("message", "预审通过，等待最终审批（模拟）");
        return Result.success(data);
    }

    @Operation(summary = "获取贷款申请列表（模拟数据）")
    @GetMapping("/list")
    public Result<Map<String, Object>> list() {
        Map<String, Object> data = new HashMap<>();
        data.put("total", 1);
        data.put("items", java.util.List.of(
                Map.of(
                        "id", 2001,
                        "loanType", "B",
                        "applyAmount", new BigDecimal("15000.00"),
                        "loanPurpose", "市集摊位物料采购",
                        "status", "审批中",
                        "createTime", "2026-09-15 10:30:00"
                )
        ));
        return Result.success(data);
    }
}
