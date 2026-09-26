package com.icbc.qingqi.module.bookkeeping.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icbc.qingqi.common.BizException;
import com.icbc.qingqi.common.ErrorCode;
import com.icbc.qingqi.module.bookkeeping.dto.BookkeepingRecordDTO;
import com.icbc.qingqi.module.bookkeeping.entity.BizBookkeepingRecord;
import com.icbc.qingqi.module.bookkeeping.entity.BizCashflowReport;
import com.icbc.qingqi.module.bookkeeping.mapper.BizBookkeepingRecordMapper;
import com.icbc.qingqi.module.bookkeeping.mapper.BizCashflowReportMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 简易记账服务
 * <p>
 * 覆盖 B-1：
 * - 记账列表与新增
 * - 现金流报表（按 biz_bookkeeping_record 聚合，实时计算并落库 biz_cashflow_report）
 */
@Slf4j
@Service
public class BookkeepingService {

    private final BizBookkeepingRecordMapper recordMapper;
    private final BizCashflowReportMapper reportMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BookkeepingService(BizBookkeepingRecordMapper recordMapper,
                              BizCashflowReportMapper reportMapper) {
        this.recordMapper = recordMapper;
        this.reportMapper = reportMapper;
    }

    // ============================================================
    //  B-1 记账列表
    // ============================================================

    public List<BizBookkeepingRecord> listRecords(Long userId, String type, Integer month) {
        LambdaQueryWrapper<BizBookkeepingRecord> wrapper = new LambdaQueryWrapper<BizBookkeepingRecord>()
                .eq(BizBookkeepingRecord::getUserId, userId)
                .orderByDesc(BizBookkeepingRecord::getHappenDate);
        if (type != null && !type.isEmpty()) {
            wrapper.eq(BizBookkeepingRecord::getRecordType, type);
        }
        if (month != null) {
            LocalDate start = LocalDate.now().withDayOfMonth(1).plusMonths(month);
            LocalDate end = start.plusMonths(1).minusDays(1);
            wrapper.between(BizBookkeepingRecord::getHappenDate, start, end);
        }
        return recordMapper.selectList(wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public BizBookkeepingRecord addRecord(Long userId, BookkeepingRecordDTO dto) {
        BizBookkeepingRecord record = new BizBookkeepingRecord();
        record.setUserId(userId);
        record.setRecordType(dto.getRecordType());
        record.setCategory(dto.getCategory());
        record.setAmount(dto.getAmount());
        record.setHappenDate(dto.getHappenDate());
        record.setDescription(dto.getDescription());
        record.setRelatedParty(dto.getRelatedParty());
        record.setSource("MANUAL");
        record.setIsConfirmed(1);
        recordMapper.insert(record);
        return record;
    }

    // ============================================================
    //  B-1 现金流报表
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public BizCashflowReport generateCashflowReport(Long userId, String period) {
        String p = period != null ? period : currentPeriod();
        LocalDate start = LocalDate.parse(p + "-01");
        LocalDate end = start.plusMonths(1).minusDays(1);

        List<BizBookkeepingRecord> records = recordMapper.selectList(
                new LambdaQueryWrapper<BizBookkeepingRecord>()
                        .eq(BizBookkeepingRecord::getUserId, userId)
                        .between(BizBookkeepingRecord::getHappenDate, start, end));

        BigDecimal totalIncome = records.stream()
                .filter(r -> "INCOME".equals(r.getRecordType()))
                .map(BizBookkeepingRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpense = records.stream()
                .filter(r -> "EXPENSE".equals(r.getRecordType()))
                .map(BizBookkeepingRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal netCashFlow = totalIncome.subtract(totalExpense);
        BigDecimal profitAmount = netCashFlow;
        BigDecimal profitMargin = totalIncome.compareTo(BigDecimal.ZERO) > 0
                ? profitAmount.multiply(new BigDecimal("100")).divide(totalIncome, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 预警等级：净现金流为负→CRITICAL；利润率<10%→WARNING；否则 NORMAL
        String warningLevel;
        String warningContent;
        if (netCashFlow.compareTo(BigDecimal.ZERO) < 0) {
            warningLevel = "CRITICAL";
            warningContent = "本期支出大于收入" + netCashFlow.abs() + "元，现金流为负，存在财务风险，建议优先削减非必要支出。";
        } else if (profitMargin.compareTo(new BigDecimal("10")) < 0) {
            warningLevel = "WARNING";
            warningContent = "本期储蓄率仅" + profitMargin + "%，低于健康水平(10%)，建议提升结余能力。";
        } else {
            warningLevel = "NORMAL";
            warningContent = "本期现金流健康，净结余" + netCashFlow + "元，储蓄率" + profitMargin + "%。";
        }

        // 分类汇总（支出）
        Map<String, BigDecimal> expenseByCategory = records.stream()
                .filter(r -> "EXPENSE".equals(r.getRecordType()))
                .collect(Collectors.groupingBy(BizBookkeepingRecord::getCategory,
                        Collectors.mapping(BizBookkeepingRecord::getAmount,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
        Map<String, BigDecimal> incomeByCategory = records.stream()
                .filter(r -> "INCOME".equals(r.getRecordType()))
                .collect(Collectors.groupingBy(BizBookkeepingRecord::getCategory,
                        Collectors.mapping(BizBookkeepingRecord::getAmount,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));

        Map<String, Object> reportData = new LinkedHashMap<>();
        reportData.put("expenseByCategory", expenseByCategory);
        reportData.put("incomeByCategory", incomeByCategory);
        reportData.put("recordCount", records.size());

        String reportDataJson;
        try {
            reportDataJson = objectMapper.writeValueAsString(reportData);
        } catch (Exception e) {
            reportDataJson = "{}";
        }

        // 查找已有报表，存在则更新
        BizCashflowReport existing = reportMapper.selectOne(
                new LambdaQueryWrapper<BizCashflowReport>()
                        .eq(BizCashflowReport::getUserId, userId)
                        .eq(BizCashflowReport::getReportPeriod, p));
        if (existing != null) {
            existing.setTotalIncome(totalIncome);
            existing.setTotalExpense(totalExpense);
            existing.setNetCashFlow(netCashFlow);
            existing.setProfitAmount(profitAmount);
            existing.setProfitMargin(profitMargin);
            existing.setWarningLevel(warningLevel);
            existing.setWarningContent(warningContent);
            existing.setReportData(reportDataJson);
            existing.setGenerateTime(LocalDateTime.now());
            reportMapper.updateById(existing);
            return existing;
        }

        BizCashflowReport report = new BizCashflowReport();
        report.setUserId(userId);
        report.setReportPeriod(p);
        report.setTotalIncome(totalIncome);
        report.setTotalExpense(totalExpense);
        report.setNetCashFlow(netCashFlow);
        report.setProfitAmount(profitAmount);
        report.setProfitMargin(profitMargin);
        report.setWarningLevel(warningLevel);
        report.setWarningContent(warningContent);
        report.setReportData(reportDataJson);
        report.setGenerateTime(LocalDateTime.now());
        reportMapper.insert(report);
        return report;
    }

    private String currentPeriod() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }
}
