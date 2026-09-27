package com.icbc.qingqi.module.safety.controller;

import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.module.safety.dto.ScenarioSubmitDTO;
import com.icbc.qingqi.module.safety.dto.ScenarioSubmitVO;
import com.icbc.qingqi.module.safety.dto.ScenarioVO;
import com.icbc.qingqi.module.safety.entity.BizAntiFraudContent;
import com.icbc.qingqi.module.safety.service.ScenarioTeachingService;
import com.icbc.qingqi.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 反诈情景化教学（模拟）
 * <p>
 * 路径：/api/v1/safety/scenario/**
 * 缺口 #1 反诈情景化教学
 * 基于 biz_anti_fraud_content（content_type=SCENARIO_SIM）扩展
 * 互动问答：刷单诈骗/冒充公检法/征信洗白；选错有纠偏文案
 */
@Tag(name = "反诈情景化教学（模拟）")
@RestController
@RequestMapping("/v1/safety/scenario")
public class ScenarioTeachingController {

    private final ScenarioTeachingService service;

    public ScenarioTeachingController(ScenarioTeachingService service) {
        this.service = service;
    }

    @Operation(summary = "情景模拟内容列表",
            description = "返回 content_type=SCENARIO_SIM 的反诈情景，可按 category 过滤。")
    @GetMapping("/list")
    public Result<List<BizAntiFraudContent>> list(@RequestParam(required = false) String category) {
        return Result.success(service.listScenarios(category));
    }

    @Operation(summary = "情景详情（含问题与选项，不暴露正确答案）")
    @GetMapping("/{scenarioId}")
    public Result<ScenarioVO> detail(@PathVariable Long scenarioId) {
        return Result.success(service.getScenario(scenarioId));
    }

    @Operation(summary = "提交答案并评分",
            description = "answers=题号→选项key（A/B/C/D）；选错返回纠偏文案；作答记录持久化到 biz_fraud_detection_log（detect_result=SCENARIO_ANSWER），进度可续。")
    @PostMapping("/{scenarioId}/submit")
    public Result<ScenarioSubmitVO> submit(@PathVariable Long scenarioId,
                                           @Valid @RequestBody ScenarioSubmitDTO dto) {
        Long userId = UserContext.getUserId();
        return Result.success(service.submit(userId, scenarioId, dto));
    }
}
