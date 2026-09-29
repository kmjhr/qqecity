package com.icbc.qingqi.module.guarantee.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 退租留档提交 DTO
 * <p>
 * 结束租房（退租）时上传房屋照片留档，系统自动做照片合格审核（模拟）
 */
@Data
public class MoveoutRecordDTO {

    /** 保函 ID */
    @NotNull(message = "请选择保函")
    private Long guaranteeId;

    /** 房屋照片文件列表（JSON 数组，至少 3 张） */
    @NotNull(message = "请上传房屋照片")
    private String photos;

    /** 备注（可选） */
    private String remark;
}
