package com.icbc.qingqi.module.bookkeeping;

import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.security.UserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 创业经营赋能模块接口（占位）
 */
@RestController
@RequestMapping("/api/v1/bookkeeping")
public class BookkeepingController {

    private final BookkeepingService bookkeepingService;

    public BookkeepingController(BookkeepingService bookkeepingService) {
        this.bookkeepingService = bookkeepingService;
    }

    /** 现金流报表（模拟数据） */
    @GetMapping("/report")
    public Result<Map<String, Object>> report() {
        return Result.ok(bookkeepingService.cashflowReport(UserContext.requireUserId()));
    }
}
