package com.icbc.qingqi.module.risk.dto;

import com.icbc.qingqi.module.risk.entity.BizRiskWarning;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 风险预警聚合总览 VO（用户端）
 * <p>
 * 对应接口：GET /api/v1/risk/overview（模拟）
 * 聚合 5 类预警（均读共享表 biz_risk_warning）：
 * OVERDUE_RISK 逾期风险 / HIGH_FREQ_BORROW 高频借贷 / CREDIT_ABNORMAL 征信异常 /
 * BUDGET_OVER 预算超支 / CASHFLOW_WARNING 现金流预警
 */
@Data
public class RiskOverviewVO {

    /** 逾期风险（金融安全模块写入） */
    private List<BizRiskWarning> overdue = new ArrayList<>();

    /** 高频借贷（消费治理模块写入） */
    private List<BizRiskWarning> highFreqBorrow = new ArrayList<>();

    /** 征信异常（消费治理模块写入） */
    private List<BizRiskWarning> creditAbnormal = new ArrayList<>();

    /** 预算超支（预算消费模块写入） */
    private List<BizRiskWarning> budgetOver = new ArrayList<>();

    /** 现金流预警（经营赋能模块写入） */
    private List<BizRiskWarning> cashflow = new ArrayList<>();

    /** 未处理预警数（is_handled=0） */
    private int unhandledCount;

    /** 预警总数 */
    private int totalCount;
}
