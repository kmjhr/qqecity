package com.icbc.qingqi.module.safety.dto;

import lombok.Data;

/**
 * 主动结束演练请求（L3，模拟）
 */
@Data
public class PracticeFinishDTO {

    /** 结束原因：GIVE_UP 主动放弃等 */
    private String reason;
}
