package com.icbc.qingqi.module.guarantee.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 房屋信息实体
 * <p>
 * 对应表：biz_house
 */
@Data
@TableName("biz_house")
public class BizHouse {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 房东 ID */
    private Long landlordId;

    /** 房屋标题 */
    private String houseTitle;

    private String province;
    private String city;
    private String district;

    /** 详细地址 */
    private String address;

    /** 房屋类型：APARTMENT/HOUSE 等 */
    private String houseType;

    /** 面积（平方米） */
    private BigDecimal area;

    /** 居室数 */
    private Integer roomCount;

    /** 月租金 */
    private BigDecimal monthlyRent;

    /** 押金金额 */
    private BigDecimal depositAmount;

    /** 房屋图片（JSON 数组） */
    private String houseImages;

    /** 状态：0 下架 / 1 出租中 */
    private Integer status;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
