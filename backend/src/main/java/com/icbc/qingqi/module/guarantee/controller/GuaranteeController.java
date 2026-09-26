package com.icbc.qingqi.module.guarantee.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.module.guarantee.dto.GuaranteeApplyDTO;
import com.icbc.qingqi.module.guarantee.dto.GuaranteeApplicationVO;
import com.icbc.qingqi.module.guarantee.dto.GuaranteeVO;
import com.icbc.qingqi.module.guarantee.entity.BizHouse;
import com.icbc.qingqi.module.guarantee.service.GuaranteeService;
import com.icbc.qingqi.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 安居金融风控接口
 * <p>
 * 路径：/api/v1/guarantee/** （需要登录）
 * 主链路：申请 → 房东确认 → AI复审 → 缴费开函 → 查询
 */
@Tag(name = "安居金融风控")
@RestController
@RequestMapping("/v1/guarantee")
public class GuaranteeController {

    private final GuaranteeService guaranteeService;

    public GuaranteeController(GuaranteeService guaranteeService) {
        this.guaranteeService = guaranteeService;
    }

    @Operation(summary = "提交保函申请")
    @PostMapping("/apply")
    public Result<GuaranteeApplicationVO> apply(@Valid @RequestBody GuaranteeApplyDTO dto) {
        Long userId = UserContext.getUserId();
        return Result.success(guaranteeService.apply(userId, dto));
    }

    @Operation(summary = "房东确认保函申请")
    @PutMapping("/application/{id}/landlord-confirm")
    public Result<GuaranteeApplicationVO> landlordConfirm(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        return Result.success(guaranteeService.landlordConfirm(userId, id));
    }

    @Operation(summary = "人工复核通过（演示自动通过）")
    @PutMapping("/application/{id}/manual-pass")
    public Result<GuaranteeApplicationVO> manualPass(@PathVariable Long id) {
        return Result.success(guaranteeService.manualReviewPass(id));
    }

    @Operation(summary = "缴纳保函费并开立电子保函")
    @PostMapping("/application/{id}/pay")
    public Result<GuaranteeVO> payAndIssue(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        return Result.success(guaranteeService.payAndIssue(userId, id));
    }

    @Operation(summary = "分页查询保函申请列表")
    @GetMapping("/application/page")
    public Result<Page<GuaranteeApplicationVO>> pageApplications(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status) {
        Long userId = UserContext.getUserId();
        return Result.success(guaranteeService.pageApplications(userId, pageNum, pageSize, status));
    }

    @Operation(summary = "获取保函申请详情")
    @GetMapping("/application/{id}")
    public Result<GuaranteeApplicationVO> getApplication(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        return Result.success(guaranteeService.getApplicationDetail(userId, id));
    }

    @Operation(summary = "分页查询保函列表")
    @GetMapping("/page")
    public Result<Page<GuaranteeVO>> pageGuarantees(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status) {
        Long userId = UserContext.getUserId();
        return Result.success(guaranteeService.pageGuarantees(userId, pageNum, pageSize, status));
    }

    @Operation(summary = "获取保函详情")
    @GetMapping("/{id}")
    public Result<GuaranteeVO> getGuarantee(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        return Result.success(guaranteeService.getGuaranteeDetail(userId, id));
    }

    @Operation(summary = "获取可选房屋列表")
    @GetMapping("/houses")
    public Result<List<BizHouse>> listHouses() {
        return Result.success(guaranteeService.listHouses());
    }
}
