package com.icbc.qingqi.module.safety.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 反诈对话演练记录实体
 * <p>
 * 对应表：biz_scenario_practice（L3 对话式反诈演练，模拟）
 * result：SAFE识破 / LURED被诱骗 / TIMEOUT超时 / FINISHED主动结束
 */
@Data
@TableName("biz_scenario_practice")
public class BizScenarioPractice {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 演练编号，如 PRAC20260927001 */
    private String practiceNo;

    /** 演练用户ID */
    private Long userId;

    /** 情景ID（biz_anti_fraud_content.id，content_type=SCENARIO_DIALOG） */
    private Long scenarioId;

    /** 情景标题（冗余） */
    private String scenarioTitle;

    /** 已完成消息回合数（含诈骗方与用户） */
    private Integer roundCount;

    /** 结果：SAFE/LURED/TIMEOUT/FINISHED */
    private String result;

    /** 综合风险分 0-100（越低越安全） */
    private Integer riskScore;

    /** 判定结论（AI 复盘摘要） */
    private String resultDesc;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
