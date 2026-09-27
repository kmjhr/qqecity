package com.icbc.qingqi.module.consumption.controller;

import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.module.consumption.dto.FinanceProductVO;
import com.icbc.qingqi.module.consumption.service.FinanceProductService;
import com.icbc.qingqi.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 理财产品匹配与推荐（仅低风险，模拟）
 * <p>
 * 路径：/api/v1/consumption/finance-product/**
 * 缺口 #7 理财匹配与风险测评（产品池部分）
 * 合规口径：理财非存款、产品有风险、工行仅代销
 */
@Tag(name = "理财匹配与推荐（仅低风险，模拟）")
@RestController
@RequestMapping("/v1/consumption/finance-product")
public class FinanceProductController {

    private final FinanceProductService service;

    public FinanceProductController(FinanceProductService service) {
        this.service = service;
    }

    @Operation(summary = "列出全部产品（不强制测评）",
            description = "返回全部 ACTIVE 状态的低风险产品")
    @GetMapping
    public Result<List<FinanceProductVO>> list() {
        return Result.success(service.listAll());
    }

    @Operation(summary = "按当前用户风险等级推荐产品",
            description = "强制测评：未完成有效测评返回 1001。仅推荐适配当前用户风险等级的产品。")
    @GetMapping("/recommend")
    public Result<List<FinanceProductVO>> recommend() {
        Long userId = UserContext.getUserId();
        return Result.success(service.recommendByUser(userId));
    }

    @Operation(summary = "按指定风险等级推荐产品（管理端/演示用）",
            description = "riskLevel: CONSERVATIVE/STEADY/BALANCED")
    @GetMapping("/recommend/by-level")
    public Result<List<FinanceProductVO>> recommendByLevel(
            @Parameter(description = "风险等级") @RequestParam String riskLevel) {
        return Result.success(service.recommendByLevel(riskLevel));
    }

    @Operation(summary = "产品详情")
    @GetMapping("/{id}")
    public Result<FinanceProductVO> detail(@PathVariable Long id) {
        return Result.success(service.detail(id));
    }

    @Operation(summary = "购买演示",
            description = "强制测评 + 产品适配校验；返回模拟购买链接 + 风险提示。不发起真实扣款。")
    @PostMapping("/{id}/apply-demo")
    public Result<FinanceProductService.ApplyResult> applyDemo(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        return Result.success(service.applyDemo(userId, id));
    }
}
