package com.icbc.qingqi.module.safety.controller;

import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.module.safety.dto.FraudDetectDTO;
import com.icbc.qingqi.module.safety.entity.BizAntiFraudAlert;
import com.icbc.qingqi.module.safety.entity.BizAntiFraudContent;
import com.icbc.qingqi.module.safety.entity.BizFraudDetectionLog;
import com.icbc.qingqi.module.safety.service.SafetyService;
import com.icbc.qingqi.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模块5 - 青年金融安全（安全教育平台风格）
 * <p>
 * 路径：/api/v1/safety/**
 * S-3 实时反诈预警、S-4 典型反诈案例（与平台业务强关联前置）
 */
@Tag(name = "青年金融安全")
@RestController
@RequestMapping("/v1/safety")
public class SafetyController {

    private final SafetyService safetyService;

    public SafetyController(SafetyService safetyService) {
        this.safetyService = safetyService;
    }

    @Operation(summary = "S-3 实时反诈预警列表（人工维护·模拟实时）")
    @GetMapping("/alerts")
    public Result<List<BizAntiFraudAlert>> listAlerts(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String scene) {
        return Result.success(safetyService.listAlerts(level, scene));
    }

    @Operation(summary = "S-4 典型反诈案例（与青启e城业务强关联的前置）")
    @GetMapping("/featured-cases")
    public Result<List<BizAntiFraudContent>> featuredCases() {
        return Result.success(safetyService.featuredCases());
    }

    @Operation(summary = "S-1 反诈内容列表")
    @GetMapping("/anti-fraud/list")
    public Result<List<BizAntiFraudContent>> listAntiFraud(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String contentType) {
        return Result.success(safetyService.listAntiFraud(category, contentType));
    }

    @Operation(summary = "S-1 反诈内容详情")
    @GetMapping("/anti-fraud/{id}")
    public Result<BizAntiFraudContent> antiFraudDetail(@PathVariable Long id) {
        return Result.success(safetyService.getAntiFraudDetail(id));
    }

    @Operation(summary = "S-2 骗局甄别（话术命中即拦截警示并记录）")
    @PostMapping("/fraud-detect")
    public Result<BizFraudDetectionLog> fraudDetect(@Valid @RequestBody FraudDetectDTO dto) {
        return Result.success(safetyService.detectFraud(UserContext.getUserId(), dto));
    }
}
