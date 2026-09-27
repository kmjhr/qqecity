package com.icbc.qingqi.module.guarantee.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.module.guarantee.dto.*;
import com.icbc.qingqi.module.guarantee.service.GuaranteeClaimService;
import com.icbc.qingqi.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 模块1扩展 - 保函索赔闭环（G-6）
 * <p>
 * 路径：/api/v1/guarantee/claims/**
 * 覆盖 G-6：房东发起索赔 → AI 初审 → 低风险速赔 / 申辩期 → 人工复核 → 赔付/拒绝
 * <p>
 * 所有银行能力（赔付）均为模拟桩，演示数据标注"模拟"。
 */
@Tag(name = "安居金融风控 - 保函索赔")
@RestController
@RequestMapping("/v1/guarantee/claims")
public class GuaranteeClaimController {

    private final GuaranteeClaimService claimService;

    public GuaranteeClaimController(GuaranteeClaimService claimService) {
        this.claimService = claimService;
    }

    // ============================================================
    //  G-6-1 房东发起索赔
    // ============================================================

    @Operation(summary = "G-6-1 房东发起索赔",
            description = "房东提交索赔金额、原因、证据材料（evidence_files JSON 数组），系统创建索赔记录并自动触发 AI 初审。")
    @PostMapping
    public Result<ClaimVO> submitClaim(@Valid @RequestBody ClaimSubmitDTO dto) {
        Long userId = UserContext.getUserId();
        return Result.success(claimService.submitClaim(userId, dto));
    }

    // ============================================================
    //  G-6-3 租客提交申辩
    // ============================================================

    @Operation(summary = "G-6-3 租客提交申辩",
            description = "申辩期内租客提交反证内容（defense_content），提交后索赔转入人工复核 MANUAL_REVIEW。")
    @PutMapping("/{id}/defense")
    public Result<ClaimVO> submitDefense(
            @Parameter(description = "索赔 ID") @PathVariable Long id,
            @Valid @RequestBody ClaimDefenseDTO dto) {
        Long userId = UserContext.getUserId();
        return Result.success(claimService.submitDefense(userId, id, dto));
    }

    // ============================================================
    //  G-6-4 banker 人工复核
    // ============================================================

    @Operation(summary = "G-6-4 banker 人工复核索赔",
            description = "banker01 对 MANUAL_REVIEW 状态的索赔做出裁决：APPROVED（payout_amount 赔付）/ REJECTED（reject_reason 拒绝）。")
    @PutMapping("/{id}/review")
    public Result<ClaimVO> manualReview(
            @Parameter(description = "索赔 ID") @PathVariable Long id,
            @Valid @RequestBody ClaimReviewDTO dto) {
        Long userId = UserContext.getUserId();
        return Result.success(claimService.manualReview(userId, id, dto));
    }

    // ============================================================
    //  G-6-5 查询
    // ============================================================

    @Operation(summary = "G-6-5 分页查询索赔列表",
            description = "房东可见自己发起的索赔，租客可见针对自己的索赔。")
    @GetMapping
    public Result<Page<ClaimVO>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status) {
        Long userId = UserContext.getUserId();
        return Result.success(claimService.pageClaims(userId, pageNum, pageSize, status));
    }

    @Operation(summary = "G-6-5 查询索赔详情")
    @GetMapping("/{id}")
    public Result<ClaimVO> detail(
            @Parameter(description = "索赔 ID") @PathVariable Long id) {
        Long userId = UserContext.getUserId();
        return Result.success(claimService.getClaimDetail(userId, id));
    }

    // ============================================================
    //  人工复核队列（banker 专用）
    // ============================================================

    @Operation(summary = "查询人工复核队列",
            description = "banker 查询所有 MANUAL_REVIEW 状态的索赔，按提交时间升序排列。")
    @GetMapping("/manual-review-queue")
    public Result<Page<ClaimVO>> manualReviewQueue(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(claimService.pageManualReviewQueue(pageNum, pageSize));
    }

    // ============================================================
    //  辅助：索赔状态流转说明
    // ============================================================

    @Operation(summary = "索赔状态流转说明",
            description = "返回 7 态流转：已提交→AI初审→申辩期→人工复核→已赔付/已拒绝→已结案，供前端展示。")
    @GetMapping("/status-flow")
    public Result<Map<String, Object>> statusFlow() {
        return Result.success(GuaranteeClaimService.claimStatusFlow());
    }
}
