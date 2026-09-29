package com.icbc.qingqi.module.guarantee.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 退租留档审核实体
 * <p>
 * 对应表：biz_moveout_record
 * 结束租房（退租）时租客上传房屋照片留档，系统对照片做 AI 合格审核（模拟）
 * <p>
 * check_result：PASS-合格留档 / REVIEW-需补拍或人工复核
 */
@Data
@TableName("biz_moveout_record")
public class BizMoveoutRecord {

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 留档编号 */
    private String recordNo;

    /** 保函 ID */
    private Long guaranteeId;

    /** 保函编号（冗余） */
    private String guaranteeNo;

    /** 租客 ID */
    private Long tenantId;

    /** 房东 ID */
    private Long landlordId;

    /** 房屋 ID */
    private Long houseId;

    /** 房屋照片文件列表（JSON 数组） */
    private String photosJson;

    /** 照片审核结果：PASS合格留档/REVIEW需补拍或人工复核 */
    private String checkResult;

    /** 照片审核明细 */
    private String checkDetail;

    /** 房东确认：PENDING待确认/CONFIRMED已确认无需索赔 */
    private String landlordConfirm;

    /** 房东确认时间 */
    private LocalDateTime landlordConfirmTime;

    /** 房东确认备注 */
    private String landlordConfirmRemark;

    /** 备注 */
    private String remark;

    /** 逻辑删除：0-存在，1-删除 */
    @TableLogic
    private Integer deleted;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
