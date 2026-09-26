package com.icbc.qingqi.module.bookkeeping;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 模块3 创业经营赋能（占位）
 * 一期仅提供现金流报表模拟接口，AI记账与RAG对话在二期完善。
 */
@Service
public class BookkeepingService {

    public Map<String, Object> cashflowReport(Long userId) {
        return Map.of(
                "userId", userId,
                "period", "2026-09（模拟）",
                "totalIncome", new BigDecimal("6800.00"),
                "totalExpense", new BigDecimal("3560.00"),
                "netCashflow", new BigDecimal("3240.00"),
                "profitEstimate", new BigDecimal("2180.00"),
                "items", List.of(
                        Map.of("date", "2026-09-01", "type", "收入", "amount", "3500.00", "note", "经营收款（模拟）"),
                        Map.of("date", "2026-09-15", "type", "收入", "amount", "3300.00", "note", "经营收款（模拟）"),
                        Map.of("date", "2026-09-08", "type", "支出", "amount", "1200.00", "note", "原料采购（模拟）"),
                        Map.of("date", "2026-09-20", "type", "支出", "amount", "2360.00", "note", "场地与物料（模拟）")),
                "tip", "该接口为占位实现，AI记账、政策推送与RAG问答在二期（Sprint 4/7）完善。");
    }
}
