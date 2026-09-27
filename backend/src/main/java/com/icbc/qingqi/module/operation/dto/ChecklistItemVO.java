package com.icbc.qingqi.module.operation.dto;

import lombok.Data;

/**
 * 个转企自查条件项 VO（供前端渲染问卷）
 */
@Data
public class ChecklistItemVO {

    /** 条件编号 1-6 */
    private Integer id;

    /** 条件标题 */
    private String title;

    /** 条件说明 */
    private String description;

    /** 选项：true=是 / false=否 */
    private String yesLabel;
    private String noLabel;
}
