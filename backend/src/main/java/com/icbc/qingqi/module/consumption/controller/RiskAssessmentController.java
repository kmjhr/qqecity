package com.icbc.qingqi.module.consumption.controller;

import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.module.consumption.dto.RiskAssessmentSubmitDTO;
import com.icbc.qingqi.module.consumption.dto.RiskAssessmentVO;
import com.icbc.qingqi.module.consumption.dto.RiskQuestionVO;
import com.icbc.qingqi.module.consumption.service.RiskAssessmentService;
import com.icbc.qingqi.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 风险测评
 * <p>
 * 路径：/api/v1/consumption/risk-assessment/**
 * 缺口 #7 理财匹配与风险测评（前置）
 * 10 题问卷 → 三档风险等级 → 1 年有效期
 */
@Tag(name = "风险测评")
@RestController
@RequestMapping("/v1/consumption/risk-assessment")
public class RiskAssessmentController {

    private final RiskAssessmentService service;

    public RiskAssessmentController(RiskAssessmentService service) {
        this.service = service;
    }

    @Operation(summary = "获取风险测评问卷（10 题）")
    @GetMapping("/questionnaire")
    public Result<List<RiskQuestionVO>> questionnaire() {
        return Result.success(service.getQuestionnaire());
    }

    @Operation(summary = "提交测评答案",
            description = "answers 字段为 {1:\"A\", 2:\"B\", ...} 形式；10-18分→保守型，19-30分→稳健型，31-40分→平衡型")
    @PostMapping("/submit")
    public Result<RiskAssessmentVO> submit(@Valid @RequestBody RiskAssessmentSubmitDTO dto) {
        Long userId = UserContext.getUserId();
        return Result.success(service.submit(userId, dto));
    }

    @Operation(summary = "查询当前用户最新测评结果")
    @GetMapping("/latest")
    public Result<RiskAssessmentVO> latest() {
        Long userId = UserContext.getUserId();
        return Result.success(service.latest(userId));
    }

    @Operation(summary = "查询历史测评记录")
    @GetMapping("/history")
    public Result<List<RiskAssessmentVO>> history() {
        Long userId = UserContext.getUserId();
        return Result.success(service.history(userId));
    }
}
