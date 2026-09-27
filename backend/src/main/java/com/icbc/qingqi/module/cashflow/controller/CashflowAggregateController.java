package com.icbc.qingqi.module.cashflow.controller;

import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.module.cashflow.dto.AggregateReportVO;
import com.icbc.qingqi.module.cashflow.dto.ChannelAuthDTO;
import com.icbc.qingqi.module.cashflow.dto.ChannelAuthVO;
import com.icbc.qingqi.module.cashflow.service.CashflowAggregateService;
import com.icbc.qingqi.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 多渠道流水聚合（步骤 7·智能中台 缺口 #19）
 * <p>
 * 路径：/api/v1/cashflow/aggregate/**
 * 演示级：授权勾选（工行收款码/微信/支付宝/淘宝）→ 生成聚合流水，支持解绑/删除（演示合规）
 * 来源标注"已授权聚合"，全程"模拟"口径
 */
@Tag(name = "多渠道流水聚合")
@RestController
@RequestMapping("/v1/cashflow/aggregate")
public class CashflowAggregateController {

    private final CashflowAggregateService service;

    public CashflowAggregateController(CashflowAggregateService service) {
        this.service = service;
    }

    @Operation(summary = "查询当前渠道授权状态")
    @GetMapping("/auth")
    public Result<ChannelAuthVO> getAuth() {
        return Result.success(service.getAuth(UserContext.getUserId()));
    }

    @Operation(summary = "授权勾选渠道并生成聚合流水",
            description = "勾选 ICBC_QR/WECHAT/ALIPAY/TAOBAO 后即时生成模拟聚合流水，TTL 24h；"
                    + "全程'已授权聚合'标注；可解绑")
    @PostMapping("/auth")
    public Result<ChannelAuthVO> authorize(@Valid @RequestBody ChannelAuthDTO dto) {
        return Result.success(service.authorize(UserContext.getUserId(), dto));
    }

    @Operation(summary = "解绑指定渠道（演示合规）")
    @DeleteMapping("/auth/{channelCode}")
    public Result<ChannelAuthVO> unbind(@PathVariable String channelCode) {
        return Result.success(service.unbind(UserContext.getUserId(), channelCode));
    }

    @Operation(summary = "拉取聚合流水报表",
            description = "总收入/总退款/净流水 + 渠道汇总 + 最近 50 条流水（来源'已授权聚合'）")
    @GetMapping("/report")
    public Result<AggregateReportVO> report() {
        return Result.success(service.getReport(UserContext.getUserId()));
    }
}
