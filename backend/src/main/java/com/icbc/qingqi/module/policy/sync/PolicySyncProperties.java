package com.icbc.qingqi.module.policy.sync;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 政策定时同步配置
 * <p>
 * 对应 application.yml policy.sync.*
 * sources 可配置多个官方栏目页，每个源独立抓取、独立容错
 */
@Data
@Component
@ConfigurationProperties(prefix = "policy.sync")
public class PolicySyncProperties {

    /** 是否启用定时同步 */
    private boolean enabled = true;

    /** 定时表达式（默认每天 02:00） */
    private String cron = "0 0 2 * * ?";

    /** 官方栏目源列表 */
    private List<Source> sources = new ArrayList<>();

    @Data
    public static class Source {
        /** 源标识（生成 policy_no 前缀） */
        private String name;
        /** 政策地区（写入 region 字段） */
        private String region;
        /** 官方栏目页 URL */
        private String url;
    }
}
