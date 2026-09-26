package com.icbc.qingqi.module.safety.controller;

import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 模块5 - 青年金融安全（占位骨架）
 * <p>
 * 路径：/api/v1/safety/**
 * 功能：征信监测、风险预警、反诈学习、金融安全教育等
 * <p>
 * 当前为占位实现，返回模拟数据，后续迭代逐步接入真实业务逻辑
 */
@Tag(name = "青年金融安全")
@RestController
@RequestMapping("/v1/safety")
public class SafetyController {

    @Operation(summary = "获取风险概览（模拟数据）")
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        Long userId = UserContext.getUserId();
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("creditStatus", "良好");
        data.put("riskLevel", "低风险");
        data.put("activeAlerts", 0);
        data.put("learnScore", 85);
        return Result.success(data);
    }

    @Operation(summary = "获取风险预警列表（模拟数据）")
    @GetMapping("/alerts")
    public Result<Map<String, Object>> alerts() {
        Map<String, Object> data = new HashMap<>();
        data.put("total", 0);
        data.put("items", java.util.List.of());
        data.put("message", "暂无风险预警，继续保持良好的金融习惯！");
        return Result.success(data);
    }

    @Operation(summary = "获取反诈学习记录（模拟数据）")
    @GetMapping("/anti-fraud")
    public Result<Map<String, Object>> antiFraud() {
        Map<String, Object> data = new HashMap<>();
        data.put("totalLearned", 5);
        data.put("totalScore", 425);
        data.put("avgScore", 85);
        data.put("recentRecords", java.util.List.of(
                Map.of("id", 1, "learnType", "情景模拟",
                        "contentId", "AF001", "finishStatus", "已完成",
                        "score", 90, "learnTime", "2026-09-20 14:30:00"),
                Map.of("id", 2, "learnType", "财商课程",
                        "contentId", "FQ001", "finishStatus", "已完成",
                        "score", 80, "learnTime", "2026-09-18 09:15:00")
        ));
        return Result.success(data);
    }

    @Operation(summary = "征信监测查询（模拟）")
    @GetMapping("/credit-monitor")
    public Result<Map<String, Object>> creditMonitor() {
        Map<String, Object> data = new HashMap<>();
        data.put("queryTime", "2026-09-25 10:00:00");
        data.put("creditSummary", "征信状态良好，无异常记录");
        data.put("abnormalType", "");
        data.put("alertFlag", 0);
        data.put("message", "本次为软查询，不影响征信记录（模拟）");
        return Result.success(data);
    }
}
