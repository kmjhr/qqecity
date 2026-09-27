package com.icbc.qingqi.module.operation.controller;

import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.module.operation.dto.ChecklistItemVO;
import com.icbc.qingqi.module.operation.dto.FinanceContentVO;
import com.icbc.qingqi.module.operation.dto.IndividualToCompanyChecklistDTO;
import com.icbc.qingqi.module.operation.dto.IndividualToCompanyResultVO;
import com.icbc.qingqi.module.operation.service.FinanceContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 财税科普 + 个转企引导
 * <p>
 * 路径：/api/v1/operation/finance/**
 * 缺口 #25 财税科普与个转企引导（简化）
 * 合规口径：内容仅为科普参考，不构成税务/法律意见
 */
@Tag(name = "财税科普与个转企引导")
@RestController
@RequestMapping("/v1/operation/finance")
public class FinanceContentController {

    private final FinanceContentService financeContentService;

    public FinanceContentController(FinanceContentService financeContentService) {
        this.financeContentService = financeContentService;
    }

    // ============================================================
    //  财税科普内容
    // ============================================================

    @Operation(summary = "查询财税科普内容", description = "category: TAX_POPULARIZATION-税务优惠 / INVOICE_GUIDE-发票常识；不传则返回全部")
    @GetMapping("/content")
    public Result<List<FinanceContentVO>> list(
            @Parameter(description = "内容分类") @RequestParam(required = false) String category) {
        return Result.success(financeContentService.listByCategory(category));
    }

    @Operation(summary = "财税科普内容详情")
    @GetMapping("/content/{id}")
    public Result<FinanceContentVO> detail(@PathVariable Long id) {
        return Result.success(financeContentService.detail(id));
    }

    // ============================================================
    //  个转企引导
    // ============================================================

    @Operation(summary = "获取个转企自查条件清单", description = "返回 6 项自查条件，供前端渲染问卷")
    @GetMapping("/individual-to-company/checklist")
    public Result<List<ChecklistItemVO>> checklist() {
        return Result.success(financeContentService.getChecklist());
    }

    @Operation(summary = "提交个转企自查答案",
            description = "answers 字段为 {1:true, 2:false, ...} 形式；命中 4+ 项→建议转企，2-3 项→可选，0-1 项→暂不建议")
    @PostMapping("/individual-to-company/self-check")
    public Result<IndividualToCompanyResultVO> selfCheck(@Valid @RequestBody IndividualToCompanyChecklistDTO dto) {
        return Result.success(financeContentService.selfCheck(dto));
    }
}
