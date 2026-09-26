package com.icbc.qingqi.module.guarantee.controller;

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
 * 模块1 - 安居金融风控（占位骨架）
 * <p>
 * 路径：/api/v1/guarantee/**
 * 功能：租住无忧保函申请、租赁合同上传、保函查询、违约理赔等
 * <p>
 * 当前为占位实现，返回模拟数据，后续迭代逐步接入真实业务逻辑
 */
@Tag(name = "安居金融风控")
@RestController
@RequestMapping("/v1/guarantee")
public class GuaranteeController {

    @Operation(summary = "获取保函列表（模拟数据）")
    @GetMapping("/list")
    public Result<Map<String, Object>> list() {
        Long userId = UserContext.getUserId();
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("total", 1);
        data.put("items", java.util.List.of(
                Map.of(
                        "id", 1,
                        "guaranteeNo", "BH202609010001",
                        "guaranteeAmount", new BigDecimal("3000.00"),
                        "status", "有效",
                        "beneficiary", "张房东",
                        "issueDate", LocalDate.of(2026, 9, 1).toString(),
                        "expireDate", LocalDate.of(2027, 3, 1).toString()
                )
        ));
        return Result.success(data);
    }

    @Operation(summary = "申请保函（模拟）")
    @PostMapping("/apply")
    public Result<Map<String, Object>> apply(@RequestBody Map<String, Object> req) {
        Map<String, Object> data = new HashMap<>();
        data.put("applyId", 1001L);
        data.put("status", "PENDING");
        data.put("message", "保函申请已提交，等待房东确认（模拟）");
        return Result.success(data);
    }

    @Operation(summary = "获取保函详情（模拟数据）")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("guaranteeNo", "BH202609010001");
        data.put("guaranteeAmount", new BigDecimal("3000.00"));
        data.put("feeAmount", new BigDecimal("45.00"));
        data.put("status", "有效");
        data.put("beneficiary", "张房东");
        data.put("houseAddress", "北京市朝阳区示范小区1号楼101室");
        data.put("monthlyRent", new BigDecimal("3000.00"));
        data.put("leaseStart", LocalDate.of(2026, 9, 1).toString());
        data.put("leaseEnd", LocalDate.of(2027, 3, 1).toString());
        data.put("issueDate", LocalDate.of(2026, 9, 1).toString());
        data.put("expireDate", LocalDate.of(2027, 3, 1).toString());
        return Result.success(data);
    }
}
