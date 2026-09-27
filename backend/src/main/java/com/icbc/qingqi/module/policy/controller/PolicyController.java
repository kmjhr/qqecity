package com.icbc.qingqi.module.policy.controller;

import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.module.policy.dto.PolicyVO;
import com.icbc.qingqi.module.policy.service.PolicyService;
import com.icbc.qingqi.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 政策智能匹配
 * <p>
 * 路径：/api/v1/policy/**
 * 缺口 #3 人才安居政策匹配 + 缺口 #4 创业贴息政策推送
 */
@Tag(name = "政策智能匹配")
@RestController
@RequestMapping("/v1/policy")
public class PolicyController {

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @Operation(summary = "按人群资质匹配政策",
            description = "根据当前用户 userType（STUDENT/GRADUATE/ENTREPRENEUR/OTHER）匹配 biz_policy.target_crowd，返回全部政策并标注 matched 字段。")
    @GetMapping("/match")
    public Result<List<PolicyVO>> match() {
        Long userId = UserContext.getUserId();
        return Result.success(policyService.matchPolicies(userId));
    }

    @Operation(summary = "仅返回匹配当前用户的政策")
    @GetMapping("/matched")
    public Result<List<PolicyVO>> matched() {
        Long userId = UserContext.getUserId();
        return Result.success(policyService.matchedPolicies(userId));
    }

    @Operation(summary = "按类型筛选政策", description = "policyType: HOUSING-安居 / ENTREPRENEUR-创业贴息，不传则返回全部")
    @GetMapping
    public Result<List<PolicyVO>> list(
            @Parameter(description = "政策类型") @RequestParam(required = false) String policyType) {
        return Result.success(policyService.listByType(policyType));
    }

    @Operation(summary = "政策详情")
    @GetMapping("/{id}")
    public Result<PolicyVO> detail(@PathVariable Long id) {
        return Result.success(policyService.detail(id));
    }

    @Operation(summary = "智能推送匹配政策（站内信）",
            description = "将当前用户匹配的政策以站内信推送，返回匹配的政策列表。")
    @PostMapping("/push")
    public Result<List<PolicyVO>> push() {
        Long userId = UserContext.getUserId();
        return Result.success(policyService.pushToUser(userId));
    }
}
