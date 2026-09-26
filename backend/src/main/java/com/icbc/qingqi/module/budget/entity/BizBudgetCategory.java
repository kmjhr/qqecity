package com.icbc.qingqi.module.budget.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预算分类实体
 * <p>
 * 对应表：biz_budget_category
 */
@Data
@TableName("biz_budget_category")
public class BizBudgetCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String categoryCode;

    private String categoryName;

    /** 分类类型：FOOD/ENTERTAINMENT/SHOPPING/TRANSPORT/OTHER */
    private String categoryType;

    private String icon;

    private Integer sortOrder;

    /** 是否系统预设：0否 1是 */
    private Integer isSystem;

    /** 状态：0禁用 1正常 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
