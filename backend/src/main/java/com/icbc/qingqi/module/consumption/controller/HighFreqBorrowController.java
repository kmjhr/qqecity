package com.icbc.qingqi.module.consumption.controller;

import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.module.consumption.service.HighFreqBorrowService;
import com.icbc.qingqi.module.risk.entity.BizRiskWarning;
import com.icbc.qingqi.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 高频借贷/非理性负债预警
 * <p>
 * 路径：/api/v1/consumption/high-freq-borrow/**
 * 缺口 #13 高频借贷/非理性负债预警
 */
@Tag(name = "高频借贷/非理性负债预警")
@RestController
@RequestMapping("/v1/consumption/high-freq-borrow")
public class HighFreqBorrowController {

    private final HighFreqBorrowService service;

    public HighFreqBorrowController(HighFreqBorrowService service) {
        this.service = service;
    }

    @Operation(summary = "检测高频借贷/非理性负债风险并落库预警",
            description = "规则：30天内申请≥3笔 / 当前APPROVED贷款≥2笔+新申请 / 月还款占月收入>50% → HIGH_FREQ_BORROW。7天同等级去重。")
    @PostMapping("/detect")
    public Result<HighFreqBorrowService.DetectResult> detect() {
        Long userId = UserContext.getUserId();
        return Result.success(service.detect(userId));
    }

    @Operation(summary = "查询历史高频借贷预警记录")
    @GetMapping("/history")
    public Result<List<BizRiskWarning>> history() {
        Long userId = UserContext.getUserId();
        return Result.success(service.listWarnings(userId));
    }
}
