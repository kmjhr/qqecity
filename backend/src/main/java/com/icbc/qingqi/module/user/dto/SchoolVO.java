package com.icbc.qingqi.module.user.dto;

import lombok.Data;

/**
 * 高校库 VO（模拟）
 * <p>
 * 注册页学校下拉框数据
 */
@Data
public class SchoolVO {

    /** 主键 ID */
    private Long id;

    /** 学校名称 */
    private String schoolName;

    /** 学校代码 */
    private String schoolCode;
}
