package com.icbc.qingqi.module.safety.controller;

import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.module.safety.dto.CreditHealthVO;
import com.icbc.qingqi.module.safety.dto.SimulateFixVO;
import com.icbc.qingqi.module.safety.service.CreditHealthService;
import com.icbc.qingqi.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * 征信健康管理（模拟）
 * <p>
 * 路径：/api/v1/safety/credit-health/**
 * 缺口 #26 征信健康管理（简化版）
 * 健康分 + 改善清单 + 模拟修复路径（标注不产生真实征信影响）
 */
@Tag(name = "征信健康管理（模拟）")
@RestController
@RequestMapping("/v1/safety/credit-health")
public class CreditHealthController {

    private final CreditHealthService service;

    public CreditHealthController(CreditHealthService service) {
        this.service = service;
    }

    @Operation(summary = "获取征信健康分与改善清单",
            description = "基于 biz_credit_report 最新报告：还款/负债率/查询/逾期四维评分 + 改善清单（标注模拟）。")
    @GetMapping
    public Result<CreditHealthVO> health() {
        Long userId = UserContext.getUserId();
        return Result.success(service.getHealth(userId));
    }

    @Operation(summary = "模拟修复路径",
            description = "展示 M1/M3/M6 分阶段信用分提升路径。重要：不向真实征信系统提交，不产生真实征信影响。")
    @PostMapping("/simulate-fix")
    public Result<SimulateFixVO> simulateFix() {
        Long userId = UserContext.getUserId();
        return Result.success(service.simulateFix(userId));
    }
}
