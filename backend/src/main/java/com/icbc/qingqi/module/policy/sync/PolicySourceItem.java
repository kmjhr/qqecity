package com.icbc.qingqi.module.policy.sync;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 从官方栏目页解析出的政策条目
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolicySourceItem {
    /** 政策标题 */
    private String title;
    /** 政策链接（绝对 URL） */
    private String url;
}
