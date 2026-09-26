package com.icbc.qingqi.module.safety.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 反诈内容实体
 * <p>
 * 对应表：biz_anti_fraud_content
 * 类型：ARTICLE/VIDEO/CASE
 */
@Data
@TableName("biz_anti_fraud_content")
public class BizAntiFraudContent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    /** 类型：ARTICLE/VIDEO/CASE */
    private String contentType;

    private String category;

    private String summary;

    private String content;

    private String coverImage;

    private Integer viewCount;

    private Integer sortOrder;

    /** 状态：0下架 1发布 */
    private Integer status;

    private LocalDateTime publishTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
