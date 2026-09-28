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

/**
 * 模拟支付中台 - 订单与收银台
 * 路径：/api/v1/pay/**
 */
@Tag(name = "模拟支付中台 - 订单/收银台")
@RestController
@RequestMapping("/v1/pay")
public class PayOrderController {

    private final PayService payService;

    public PayOrderController(PayService payService) {
        this.payService = payService;
    }

    @Operation(summary = "模拟消费下单（淘宝式，支付前反诈风控）")
    @PostMapping("/consume")
    public Result<PayOrderVO> consume(@Valid @RequestBody ConsumeDTO dto) {
        return Result.success(payService.consume(UserContext.getUserId(), dto));
    }

    @Operation(summary = "我的订单列表")
    @GetMapping("/orders")
    public Result<Page<PayOrderVO>> orders(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status) {
        return Result.success(payService.listOrders(UserContext.getUserId(), status, pageNum, pageSize));
    }

    @Operation(summary = "订单详情（收银台数据）")
    @GetMapping("/orders/{orderNo}")
    public Result<PayOrderVO> orderDetail(@PathVariable String orderNo) {
        return Result.success(payService.getOrder(UserContext.getUserId(), orderNo));
    }

    @Operation(summary = "收银台支付（钱包余额/模拟银行卡/工行e支付）")
    @PostMapping("/orders/{orderNo}/pay")
    public Result<PayOrderVO> pay(@PathVariable String orderNo, @Valid @RequestBody PayDTO dto) {
        return Result.success(payService.pay(UserContext.getUserId(), orderNo, dto));
    }

    @Operation(summary = "关闭订单（取消/超时）")
    @PostMapping("/orders/{orderNo}/close")
    public Result<PayOrderVO> close(@PathVariable String orderNo) {
        return Result.success(payService.closeOrder(UserContext.getUserId(), orderNo));
    }

    @Operation(summary = "申请退款（模拟即时到账）")
    @PostMapping("/orders/{orderNo}/refund")
    public Result<PayOrderVO> refund(@PathVariable String orderNo, @RequestParam(required = false) String reason) {
        return Result.success(payService.refund(UserContext.getUserId(), orderNo, reason));
    }
}
