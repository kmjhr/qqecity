package com.icbc.qingqi.module.cashflow.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 渠道授权状态
 */
@Data
public class ChannelAuthVO {

    /** 已授权渠道列表 */
    private List<ChannelInfo> authorizedChannels;

    /** 授权时间 */
    private LocalDateTime authTime;

    /** 是否模拟（始终 true） */
    private Boolean simulated;

    /** 提示语 */
    private String notice;

    @Data
    public static class ChannelInfo {
        /** 渠道编码：ICBC_QR/WECHAT/ALIPAY/TAOBAO */
        private String channelCode;
        /** 渠道名称 */
        private String channelName;
        /** 授权时间 */
        private LocalDateTime authAt;
        /** 是否模拟 */
        private Boolean simulated;
    }
}
