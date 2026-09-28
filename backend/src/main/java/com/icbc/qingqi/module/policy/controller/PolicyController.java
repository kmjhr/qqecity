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

    @Operation(summary = "智能匹配政策（有效期过滤 + 关键词匹配 + 自动推送）",
            description = "只返回有效期内政策；按用户人群资质+创业行为提取关键词，与政策 keyword_tags 命中即匹配；匹配结果自动写入消息中心（去重）。")
    @GetMapping("/match")
    public Result<List<PolicyVO>> match(
            @Parameter(description = "地区筛选，如 浙江/广东/全国") @RequestParam(required = false) String region) {
        Long userId = UserContext.getUserId();
        return Result.success(policyService.matchPolicies(userId, region));
    }

    @Operation(summary = "仅返回匹配当前用户的政策")
    @GetMapping("/matched")
    public Result<List<PolicyVO>> matched(
            @Parameter(description = "地区筛选，如 浙江/广东/全国") @RequestParam(required = false) String region) {
        Long userId = UserContext.getUserId();
        return Result.success(policyService.matchedPolicies(userId, region));
    }

    @Operation(summary = "按类型筛选政策", description = "policyType: HOUSING-安居 / ENTREPRENEUR-创业贴息，不传则返回全部")
    @GetMapping
    public Result<List<PolicyVO>> list(
            @Parameter(description = "政策类型") @RequestParam(required = false) String policyType,
            @Parameter(description = "地区筛选，如 浙江/广东/全国") @RequestParam(required = false) String region) {
        return Result.success(policyService.listByType(policyType, region));
    }

    @Operation(summary = "政策详情")
    @GetMapping("/{id}")
    public Result<PolicyVO> detail(@PathVariable Long id) {
        return Result.success(policyService.detail(id));
    }

    @Operation(summary = "智能推送匹配政策（站内信）",
            description = "手动推送匹配政策到消息中心（先清理旧推送再生成最新，自动推送已在匹配接口触发）。")
    @PostMapping("/push")
    public Result<List<PolicyVO>> push() {
        Long userId = UserContext.getUserId();
        return Result.success(policyService.pushToUser(userId));
    }
}
