package com.icbc.qingqi.module.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 高校库实体（模拟，注册学历核验白名单）
 * <p>
 * 对应表：biz_school
 * 注册环节「AI 学历审查」：学校 ∈ 本表（status=1 启用）才通过
 */
@Data
@TableName("biz_school")
public class BizSchool {

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学校名称 */
    private String schoolName;

    /** 学校代码 */
    private String schoolCode;

    /** 状态：0-停用，1-启用 */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
