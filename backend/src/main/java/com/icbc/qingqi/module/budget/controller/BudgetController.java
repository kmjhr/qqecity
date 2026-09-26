package com.icbc.qingqi.module.budget.controller;

import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.module.budget.dto.BudgetSettingDTO;
import com.icbc.qingqi.module.budget.dto.TransactionDTO;
import com.icbc.qingqi.module.budget.dto.TransferSavingDTO;
import com.icbc.qingqi.module.budget.entity.BizBudgetCategory;
import com.icbc.qingqi.module.budget.entity.BizBudgetSetting;
import com.icbc.qingqi.module.budget.entity.BizSavingGoal;
import com.icbc.qingqi.module.budget.service.BudgetService;
import com.icbc.qingqi.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 模块4 - 碎片消费治理
 * <p>
 * 路径：/api/v1/budget/**
 * 覆盖 C-1 ~ C-4：分类预算设置、模拟交易按 MCC 归类实时扣减、三级提醒、结余转心愿储蓄
 */
@Tag(name = "碎片消费治理")
@RestController
@RequestMapping("/v1/budget")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @Operation(summary = "C-1 设置/更新分类预算")
    @PostMapping("/setting")
    public Result<BizBudgetSetting> setBudget(@Valid @RequestBody BudgetSettingDTO dto) {
        return Result.success(budgetService.setBudget(UserContext.getUserId(), dto));
    }

    @Operation(summary = "获取当月预算列表（含三级提醒级别）")
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> budgetList(
            @RequestParam(required = false) String period) {
        return Result.success(budgetService.getBudgetList(UserContext.getUserId(), period));
    }

    @Operation(summary = "C-2 新增模拟交易（按 MCC 自动归类扣减预算，返回提醒级别）")
    @PostMapping("/transaction")
    public Result<Map<String, Object>> addTransaction(@Valid @RequestBody TransactionDTO dto) {
        return Result.success(budgetService.addTransaction(UserContext.getUserId(), dto));
    }

    @Operation(summary = "C-4 结余一键转入心愿储蓄")
    @PostMapping("/transfer-saving")
    public Result<Map<String, Object>> transferSaving(@RequestBody(required = false) TransferSavingDTO dto) {
        if (dto == null) dto = new TransferSavingDTO();
        return Result.success(budgetService.transferSaving(UserContext.getUserId(), dto));
    }

    @Operation(summary = "获取预算分类列表")
    @GetMapping("/categories")
    public Result<List<BizBudgetCategory>> categories() {
        return Result.success(budgetService.listCategories());
    }

    @Operation(summary = "获取心愿储蓄列表")
    @GetMapping("/savings")
    public Result<List<BizSavingGoal>> savings() {
        return Result.success(budgetService.listSavings(UserContext.getUserId()));
    }

    @Operation(summary = "预算概览（总预算/已用/剩余/整体提醒级别）")
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.success(budgetService.getOverview(UserContext.getUserId()));
    }
}
