package com.icbc.qingqi.module.policy.controller;

import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.module.policy.dto.PolicyPortalVO;
import com.icbc.qingqi.module.policy.service.PolicyPortalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 官方政策入口导航
 * <p>
 * 路径：/api/v1/policy/portals
 * 独立专区：汇总全国/各省官方政策入口（人社/住建房管/政务/税务/教育），点击跳转官网，支持筛选
 */
@Tag(name = "官方政策入口导航")
@RestController
@RequestMapping("/v1/policy/portals")
public class PolicyPortalController {

    private final PolicyPortalService portalService;

    public PolicyPortalController(PolicyPortalService portalService) {
        this.portalService = portalService;
    }

    @Operation(summary = "官方入口列表", description = "按地区/类型筛选官方政策入口，点击跳转官网。")
    @GetMapping
    public Result<List<PolicyPortalVO>> list(
            @Parameter(description = "地区（全国/浙江/广东…），空=全部") @RequestParam(required = false) String region,
            @Parameter(description = "入口类型（GOV_HR/GOV_HOUSING/GOV_AFFAIR/GOV_TAX/GOV_EDU），空=全部")
            @RequestParam(required = false) String portalType) {
        return Result.success(portalService.list(region, portalType));
    }

    @Operation(summary = "官方入口覆盖地区列表")
    @GetMapping("/regions")
    public Result<List<String>> regions() {
        return Result.success(portalService.regions());
    }
}
