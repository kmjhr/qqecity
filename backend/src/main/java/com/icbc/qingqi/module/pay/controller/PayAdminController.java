package com.icbc.qingqi.module.pay.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.module.pay.dto.*;
import com.icbc.qingqi.module.pay.service.PayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模拟支付中台 - 管理端（ADMIN/banker）
 * 路径：/api/v1/pay/admin/**
 */
@Tag(name = "模拟支付中台 - 管理端")
@RestController
@RequestMapping("/v1/pay/admin")
public class PayAdminController {

    private final PayService payService;

    public PayAdminController(PayService payService) {
        this.payService = payService;
    }

    @Operation(summary = "全量支付订单查询")
    @GetMapping("/orders")
    public Result<Page<PayOrderVO>> orders(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String bizType) {
        return Result.success(payService.adminOrders(status, bizType, pageNum, pageSize));
    }

    @Operation(summary = "全量支付流水")
    @GetMapping("/transactions")
    public Result<Page<PayTxnVO>> transactions(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String direction) {
        return Result.success(payService.adminTxns(direction, pageNum, pageSize));
    }

    @Operation(summary = "商户收款账户一览")
    @GetMapping("/merchant-accounts")
    public Result<List<MerchantAccountVO>> merchantAccounts() {
        return Result.success(payService.adminMerchantAccounts());
    }
}
