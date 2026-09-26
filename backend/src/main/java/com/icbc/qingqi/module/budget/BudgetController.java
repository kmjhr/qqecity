package com.icbc.qingqi.module.budget;

import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.security.UserContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 碎片消费治理模块接口
 */
@RestController
@RequestMapping("/api/v1/budget")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @Data
    public static class BudgetRequest {
        @NotBlank(message = "预算月份不能为空")
        private String budgetMonth;
        @NotBlank(message = "消费分类不能为空")
        private String category;
        @NotNull(message = "预算金额不能为空")
        private BigDecimal budgetAmount;
    }

    @Data
    public static class ExpenseRequest {
        @NotBlank(message = "商户名称不能为空")
        private String merchantName;
        @NotNull(message = "金额不能为空")
        private BigDecimal amount;
    }

    @Data
    public static class CarryRequest {
        @NotNull(message = "预算ID不能为空")
        private Long budgetId;
        private String goalName;
    }

    /** 设置/更新分类预算 */
    @PostMapping("/set")
    public Result<Budget> setBudget(@Valid @RequestBody BudgetRequest req) {
        return Result.ok(budgetService.setBudget(
                UserContext.requireUserId(), req.getBudgetMonth(), req.getCategory(), req.getBudgetAmount()));
    }

    /** 记录一笔支出（自动归类 + 扣减预算 + 分级提醒） */
    @PostMapping("/expense")
    public Result<Transaction> expense(@Valid @RequestBody ExpenseRequest req) {
        return Result.ok(budgetService.recordExpense(
                UserContext.requireUserId(), req.getMerchantName(), req.getAmount()));
    }

    /** 结余转储蓄 */
    @PostMapping("/carry-over")
    public Result<Map<String, Object>> carryOver(@Valid @RequestBody CarryRequest req) {
        return Result.ok(budgetService.carryOverToSaving(
                UserContext.requireUserId(), req.getBudgetId(), req.getGoalName()));
    }

    /** 预算列表（可按月份过滤） */
    @GetMapping("/list")
    public Result<List<Budget>> list(@RequestParam(required = false) String month) {
        return Result.ok(budgetService.listByUserMonth(UserContext.requireUserId(), month));
    }
}
