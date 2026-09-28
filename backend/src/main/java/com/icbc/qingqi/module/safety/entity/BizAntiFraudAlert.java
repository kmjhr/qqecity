package com.icbc.qingqi.module.safety.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 实时反诈预警实体
 * <p>
 * 对应表：biz_anti_fraud_alert
 * 安全教育平台风格「实时反诈预警」区，人工维护、标注模拟实时
 * relate_scene：GUARANTEE保函 / LOAN创业贷 / CREDIT征信 / WEALTH理财 / PLATFORM客服 / OTHER
 */
@Data
@TableName("biz_anti_fraud_alert")
public class BizAntiFraudAlert {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 预警标题 */
    private String title;

    /** 级别：DANGER/WARNING/INFO */
    private String alertLevel;

    /** 来源（模拟） */
    private String source;

    /** 涉及地区 */
    private String region;

    /** 预警内容 */
    private String summary;

    /** 官方链接（举报/提示入口） */
    private String linkUrl;

    /** 关联平台场景 */
    private String relateScene;

    /** 发布时间（模拟实时） */
    private LocalDateTime publishTime;

    /** 状态：1发布 0下架 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
