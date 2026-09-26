package com.icbc.qingqi.module.bookkeeping.controller;

import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.module.bookkeeping.dto.BookkeepingRecordDTO;
import com.icbc.qingqi.module.bookkeeping.entity.BizBookkeepingRecord;
import com.icbc.qingqi.module.bookkeeping.entity.BizCashflowReport;
import com.icbc.qingqi.module.bookkeeping.service.BookkeepingService;
import com.icbc.qingqi.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模块3 - 简易记账
 * <p>
 * 路径：/api/v1/bookkeeping/**
 * 覆盖 B-1：记账列表与现金流报表（对接 biz_bookkeeping_record / biz_cashflow_report）
 */
@Tag(name = "简易记账")
@RestController
@RequestMapping("/v1/bookkeeping")
public class BookkeepingController {

    private final BookkeepingService bookkeepingService;

    public BookkeepingController(BookkeepingService bookkeepingService) {
        this.bookkeepingService = bookkeepingService;
    }

    @Operation(summary = "B-1 记账列表")
    @GetMapping("/records")
    public Result<List<BizBookkeepingRecord>> listRecords(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer month) {
        return Result.success(bookkeepingService.listRecords(UserContext.getUserId(), type, month));
    }

    @Operation(summary = "B-1 新增记账记录")
    @PostMapping("/records")
    public Result<BizBookkeepingRecord> addRecord(@Valid @RequestBody BookkeepingRecordDTO dto) {
        return Result.success(bookkeepingService.addRecord(UserContext.getUserId(), dto));
    }

    @Operation(summary = "B-1 生成/获取现金流报表")
    @GetMapping("/cashflow-report")
    public Result<BizCashflowReport> cashflowReport(
            @RequestParam(required = false) String period) {
        return Result.success(bookkeepingService.generateCashflowReport(UserContext.getUserId(), period));
    }
}
