package com.icbc.qingqi.module.pay.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.module.pay.dto.*;
import com.icbc.qingqi.module.pay.service.PayService;
import com.icbc.qingqi.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 模拟支付中台 - 钱包
 * 路径：/api/v1/pay/wallet/**
 */
@Tag(name = "模拟支付中台 - 钱包")
@RestController
@RequestMapping("/v1/pay/wallet")
public class PayWalletController {

    private final PayService payService;

    public PayWalletController(PayService payService) {
        this.payService = payService;
    }

    @Operation(summary = "获取/开通我的钱包（模拟）")
    @GetMapping
    public Result<WalletVO> getWallet() {
        return Result.success(payService.getWallet(UserContext.getUserId()));
    }

    @Operation(summary = "模拟充值（银行卡/工行e支付）")
    @PostMapping("/recharge")
    public Result<Map<String, Object>> recharge(@Valid @RequestBody RechargeDTO dto) {
        return Result.success(payService.recharge(UserContext.getUserId(), dto));
    }

    @Operation(summary = "设置支付密码（模拟6位）")
    @PostMapping("/password")
    public Result<WalletVO> setPassword(@RequestParam String password) {
        return Result.success(payService.setPassword(UserContext.getUserId(), password));
    }

    @Operation(summary = "我的钱包收支流水")
    @GetMapping("/transactions")
    public Result<Page<PayTxnVO>> transactions(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String direction) {
        return Result.success(payService.walletTransactions(UserContext.getUserId(), pageNum, pageSize, direction));
    }
}
