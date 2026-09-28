package com.icbc.qingqi.module.policy.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 官方政策入口导航实体
 * <p>
 * 对应表：biz_policy_portal
 * 独立专区：汇总全国/各省人社、住建房管、政务、税务等官方入口，点击跳转官网
 */
@Data
@TableName("biz_policy_portal")
public class BizPolicyPortal {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 官网名称 */
    private String portalName;

    /** 入口类型：GOV_HR-人社 / GOV_HOUSING-住建房管 / GOV_AFFAIR-政务服务 / GOV_TAX-税务 / GOV_EDU-教育高校 / GOV_OTHER-其他 */
    private String portalType;

    /** 地区（全国/省/直辖市） */
    private String region;

    /** 官网链接 */
    private String url;

    /** 入口说明 */
    private String description;

    /** 状态：ACTIVE/INACTIVE */
    private String status;

    /** 排序 */
    private Integer sortOrder;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
