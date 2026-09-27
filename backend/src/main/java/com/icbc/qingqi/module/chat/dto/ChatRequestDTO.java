package com.icbc.qingqi.module.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户对话请求
 */
@Data
public class ChatRequestDTO {

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 500, message = "单条消息不超过 500 字")
    private String message;

    /** 场景：GENERAL/CONTRACT/FRAUD（影响 RAG 召回范围） */
    private String scene;
}
