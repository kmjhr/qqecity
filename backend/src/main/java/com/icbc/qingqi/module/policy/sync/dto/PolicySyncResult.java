package com.icbc.qingqi.module.policy.sync.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单源同步结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolicySyncResult {
    /** 源标识 */
    private String sourceName;
    /** 地区 */
    private String region;
    /** 本次新增条数 */
    private int added;
    /** 跳过条数（已存在，按链接去重） */
    private int skipped;
    /** 是否成功 */
    private boolean success;
    /** 失败原因（成功时为 null） */
    private String error;
}
