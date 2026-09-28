package com.icbc.qingqi.module.policy.sync;

import java.io.IOException;
import java.util.List;

/**
 * 政策官方源适配器：从官方栏目页抓取并解析政策条目
 */
public interface PolicySourceAdapter {

    /**
     * 抓取并解析官方栏目页，返回政策条目列表
     *
     * @param url 官方栏目页 URL
     * @return 解析出的政策条目（标题+链接）
     */
    List<PolicySourceItem> fetch(String url) throws IOException;
}
