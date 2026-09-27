package com.icbc.qingqi.module.cashflow.controller;

import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.module.cashflow.dto.AntiBrushResultVO;
import com.icbc.qingqi.module.cashflow.service.AntiBrushService;
import com.icbc.qingqi.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 防刷单三道防线规则引擎（步骤 7·智能中台 缺口 #20）
 * <p>
 * 路径：/api/v1/cashflow/anti-brush/**
 * 防线一·异常特征（集中对手/凌晨高频/金额雷同/退款率异常）
 * 防线二·跨维比对（物料采购+摊位活动 vs 流水）
 * 防线三·平台直取（演示入口标注"禁截图"）
 * 命中输出风险分与处置建议，全程"模拟"口径
 */
@Tag(name = "防刷单三道防线")
@RestController
@RequestMapping("/v1/cashflow/anti-brush")
public class AntiBrushController {

    private final AntiBrushService service;

    public AntiBrushController(AntiBrushService service) {
        this.service = service;
    }

    @Operation(summary = "执行防刷单检测",
            description = "三道防线规则引擎：① 异常特征；② 跨维比对（受托支付/记账 vs 聚合流水）；"
                    + "③ 平台直取标识（禁截图）。输出风险分 0-100 与处置建议")
    @PostMapping("/detect")
    public Result<AntiBrushResultVO> detect() {
        return Result.success(service.detect(UserContext.getUserId()));
    }

    @Operation(summary = "查询检测示例（演示样本）",
            description = "返回防刷单能力说明与演示场景描述，用于前端展示")
    @GetMapping("/overview")
    public Result<AntiBrushOverview> overview() {
        AntiBrushOverview o = new AntiBrushOverview();
        o.setSimulated(true);
        o.setDefenseLines(java.util.List.of(
                new AntiBrushOverview.DefenseLine(1, "异常特征识别",
                        "交易集中少数对手 / 凌晨高频(00-05点) / 金额雷同(≥5次) / 退款率异常(>15%)"),
                new AntiBrushOverview.DefenseLine(2, "跨维交叉比对",
                        "物料采购(受托支付) + 摊位活动记录(记账) vs 聚合流水，偏差>30% 视为存疑"),
                new AntiBrushOverview.DefenseLine(3, "平台直取",
                        "演示入口标注'平台直取禁截图'，仅返回风险分与处置建议，不返回原始数据截图")
        ));
        o.setRiskLevels("LOW(0-19) / MEDIUM(20-39) / HIGH(40-69) / CRITICAL(70-100)");
        o.setNotice("请先在「多渠道流水聚合」页授权勾选渠道，本接口基于已授权聚合流水执行检测。");
        return Result.success(o);
    }

    /**
     * 演示样本 VO
     */
    public static class AntiBrushOverview {
        public Boolean simulated;
        public java.util.List<DefenseLine> defenseLines;
        public String riskLevels;
        public String notice;

        public Boolean getSimulated() { return simulated; }
        public void setSimulated(Boolean simulated) { this.simulated = simulated; }
        public java.util.List<DefenseLine> getDefenseLines() { return defenseLines; }
        public void setDefenseLines(java.util.List<DefenseLine> defenseLines) { this.defenseLines = defenseLines; }
        public String getRiskLevels() { return riskLevels; }
        public void setRiskLevels(String riskLevels) { this.riskLevels = riskLevels; }
        public String getNotice() { return notice; }
        public void setNotice(String notice) { this.notice = notice; }

        public static class DefenseLine {
            public Integer lineNo;
            public String name;
            public String desc;
            public DefenseLine(Integer lineNo, String name, String desc) {
                this.lineNo = lineNo; this.name = name; this.desc = desc;
            }
            public Integer getLineNo() { return lineNo; }
            public String getName() { return name; }
            public String getDesc() { return desc; }
        }
    }
}
