package com.icbc.qingqi.module.policy.dto;

import lombok.Data;

/**
 * 官方政策入口 VO
 */
@Data
public class PolicyPortalVO {

    private Long id;
    /** 官网名称 */
    private String portalName;
    /** 入口类型 */
    private String portalType;
    /** 入口类型名称 */
    private String portalTypeName;
    /** 地区 */
    private String region;
    /** 官网链接 */
    private String url;
    /** 入口说明 */
    private String description;
    private Integer sortOrder;
}
