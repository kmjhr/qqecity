package com.icbc.qingqi.module.safety.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 反诈对话演练回合明细实体
 * <p>
 * 对应表：biz_scenario_round（L3 对话式反诈演练，模拟）
 * speaker：FRAUD诈骗方 / USER用户
 */
@Data
@TableName("biz_scenario_round")
public class BizScenarioRound {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 演练ID（biz_scenario_practice.id） */
    private Long practiceId;

    /** 回合序号（消息序号，1=诈骗方开场白） */
    private Integer roundNo;

    /** 发言方：FRAUD/USER */
    private String speaker;

    /** 发言内容 */
    private String content;

    /** 本回合安全分 0-100（用户发言回合才有） */
    private Integer safeScore;

    /** 命中的词库标签（逗号分隔） */
    private String hitWords;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
