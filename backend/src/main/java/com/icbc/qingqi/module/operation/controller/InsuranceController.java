package com.icbc.qingqi.module.operation.controller;

import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.module.operation.dto.InsuranceProductVO;
import com.icbc.qingqi.module.operation.service.InsuranceService;
import com.icbc.qingqi.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 保险代销匹配（模拟）
 * <p>
 * 路径：/api/v1/insurance/**
 * 缺口 #24 保险代销匹配
 * 合规口径：工行仅代销、不承保；演示用，不构成真实投保邀约
 */
@Tag(name = "保险代销匹配（模拟）")
@RestController
@RequestMapping("/v1/insurance")
public class InsuranceController {

    private final InsuranceService insuranceService;

    public InsuranceController(InsuranceService insuranceService) {
        this.insuranceService = insuranceService;
    }

    @Operation(summary = "按人群资质匹配保险产品",
            description = "根据当前用户 userType 匹配 biz_insurance_product.target_crowd，返回全部产品并标注 matched 字段。")
    @GetMapping("/match")
    public Result<List<InsuranceProductVO>> match() {
        Long userId = UserContext.getUserId();
        return Result.success(insuranceService.matchProducts(userId));
    }

    @Operation(summary = "仅返回匹配当前用户的产品")
    @GetMapping("/matched")
    public Result<List<InsuranceProductVO>> matched() {
        Long userId = UserContext.getUserId();
        return Result.success(insuranceService.matchedProducts(userId));
    }

    @Operation(summary = "按险种或场景筛选产品", description = "insuranceType: PERFORMANCE_BOND/IP_PATENT/IP_INFRINGEMENT/PROPERTY；scene: CONTRACT/IP/PROPERTY/EMPLOYER。不传则返回全部")
    @GetMapping
    public Result<List<InsuranceProductVO>> list(
            @Parameter(description = "险种") @RequestParam(required = false) String insuranceType,
            @Parameter(description = "经营场景") @RequestParam(required = false) String scene) {
        return Result.success(insuranceService.listByFilter(insuranceType, scene));
    }

    @Operation(summary = "产品详情")
    @GetMapping("/{id}")
    public Result<InsuranceProductVO> detail(@PathVariable Long id) {
        return Result.success(insuranceService.detail(id));
    }

    @Operation(summary = "跳转投保演示",
            description = "返回模拟投保链接 + 合规声明。不发起真实投保，不收集用户信息。")
    @PostMapping("/{id}/apply-demo")
    public Result<InsuranceService.InsuranceApplyResult> applyDemo(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        return Result.success(insuranceService.applyDemo(userId, id));
    }
}
