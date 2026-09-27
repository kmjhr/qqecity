package com.icbc.qingqi.module.safety.controller;

import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.module.safety.dto.*;
import com.icbc.qingqi.module.safety.service.ScenarioPracticeService;
import com.icbc.qingqi.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 反诈对话式演练接口（L3，模拟教学）
 * <p>
 * 路径：/api/v1/safety/scenario/**
 * AI 扮演诈骗分子施压，用户自由发言对抗；
 * 连续 2 回合危险 → LURED；安全分≥70 → SAFE；≥10 回合 → TIMEOUT；主动结束 → FINISHED。
 * LLM 增强复用对话引擎 AgentLLMClient（CHAT_LLM_API_KEY 由用户 .env 提供），缺 Key 自动降级本地剧本。
 */
@Tag(name = "反诈对话式演练（L3，模拟教学）")
@RestController
@RequestMapping("/v1/safety/scenario")
public class ScenarioPracticeController {

    private final ScenarioPracticeService service;

    public ScenarioPracticeController(ScenarioPracticeService service) {
        this.service = service;
    }

    @Operation(summary = "开始对话式演练",
            description = "情景须为 content_type=SCENARIO_DIALOG；返回演练编号 + AI 开场白。")
    @PostMapping("/{scenarioId}/practice/start")
    public Result<PracticeStartVO> start(@PathVariable Long scenarioId) {
        return Result.success(service.start(UserContext.getUserId(), scenarioId));
    }

    @Operation(summary = "用户发言回合（AI 接招 + 安全判定）",
            description = "content=用户自由发言；返回 AI 回复、本回合安全分、风险等级、危险提示；gameOver=true 时演练已结算。")
    @PostMapping("/practice/{practiceNo}/turn")
    public Result<PracticeTurnVO> turn(@PathVariable String practiceNo,
                                       @Valid @RequestBody PracticeTurnDTO dto) {
        return Result.success(service.turn(UserContext.getUserId(), practiceNo, dto.getContent()));
    }

    @Operation(summary = "主动结束 / 获取复盘",
            description = "reason 可选（GIVE_UP 等）；演练进行中调用会结算为 FINISHED，已结束则直接返回复盘。")
    @PostMapping("/practice/{practiceNo}/finish")
    public Result<PracticeFinishVO> finish(@PathVariable String practiceNo,
                                           @RequestBody(required = false) PracticeFinishDTO dto) {
        return Result.success(service.finish(UserContext.getUserId(), practiceNo,
                dto != null ? dto.getReason() : null));
    }

    @Operation(summary = "我的演练历史（分页，不含进行中）")
    @GetMapping("/practice/history")
    public Result<PracticeHistoryVO> history(@RequestParam(defaultValue = "1") Integer pageNum,
                                             @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(service.history(UserContext.getUserId(), pageNum, pageSize));
    }

    @Operation(summary = "演练回放（主表 + 回合明细）")
    @GetMapping("/practice/{practiceNo}")
    public Result<PracticeDetailVO> detail(@PathVariable String practiceNo) {
        return Result.success(service.detail(UserContext.getUserId(), practiceNo));
    }
}
