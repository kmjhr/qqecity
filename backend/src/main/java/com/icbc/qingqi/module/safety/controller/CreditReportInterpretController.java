package com.icbc.qingqi.module.safety.controller;

import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.module.consumption.entity.BizCreditReport;
import com.icbc.qingqi.module.safety.dto.CreditReportInterpretVO;
import com.icbc.qingqi.module.safety.service.CreditReportInterpretService;
import com.icbc.qingqi.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 征信报告智能解读（模拟）
 * <p>
 * 路径：/api/v1/safety/credit-report/**
 * 缺口 #2 征信报告智能解读
 * biz_credit_report 结构化解读：信用分/等级/逾期/查询/负债率 + 逐项评价与改进建议
 */
@Tag(name = "征信报告智能解读（模拟）")
@RestController
@RequestMapping("/v1/safety/credit-report")
public class CreditReportInterpretController {

    private final CreditReportInterpretService service;

    public CreditReportInterpretController(CreditReportInterpretService service) {
        this.service = service;
    }

    @Operation(summary = "列出本人全部模拟征信报告")
    @GetMapping("/reports")
    public Result<List<BizCreditReport>> reports() {
        Long userId = UserContext.getUserId();
        return Result.success(service.listReports(userId));
    }

    @Operation(summary = "结构化解读一份征信报告",
            description = "对指定 reportId 进行结构化解读：信用分/逾期/查询/负债率/账户数，逐项评价与改进建议。")
    @GetMapping("/{reportId}/interpret")
    public Result<CreditReportInterpretVO> interpret(@PathVariable Long reportId) {
        Long userId = UserContext.getUserId();
        return Result.success(service.interpret(userId, reportId));
    }

    @Operation(summary = "装载演示征信报告",
            description = "type=GOOD 装载「良好」示例；type=FLAWED 装载「有瑕疵」示例；模拟数据，不产生真实征信影响。")
    @PostMapping("/load-demo")
    public Result<BizCreditReport> loadDemo(@RequestParam String type) {
        Long userId = UserContext.getUserId();
        return Result.success(service.loadDemo(userId, type));
    }
}
