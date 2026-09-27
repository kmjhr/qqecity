package com.icbc.qingqi.module.cashflow.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 渠道授权请求（勾选平台）
 */
@Data
public class ChannelAuthDTO {

    /** 渠道：ICBC_QR/WECHAT/ALIPAY/TAOBAO */
    @NotEmpty(message = "至少选择一个渠道")
    private List<String> channels;
}
