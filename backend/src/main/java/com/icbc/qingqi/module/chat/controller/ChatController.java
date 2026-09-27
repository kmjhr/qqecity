package com.icbc.qingqi.module.chat.controller;

import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.module.chat.dto.ChatEngineStatusVO;
import com.icbc.qingqi.module.chat.dto.ChatMessageVO;
import com.icbc.qingqi.module.chat.dto.ChatRequestDTO;
import com.icbc.qingqi.module.chat.service.ChatService;
import com.icbc.qingqi.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 对话引擎适配层（步骤 7·智能中台）
 * <p>
 * 路径：/api/v1/chat/**
 * 鉴权：JWT
 * 双模式：local（默认，离线可用）+ agent（可选，需 API Key，本地 RAG 先召回拼入上下文，LLM 仅组织语言，回答仍附本地来源）
 * LLM 超时/报错/Key 空 → 自动回退 local；全程标注"模拟对话引擎/仅供参考"
 */
@Tag(name = "智能对话引擎")
@RestController
@RequestMapping("/v1/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @Operation(summary = "查询对话引擎状态",
            description = "前端用于显示'AI 增强未开启'提示；Key 为空时 agent 自动禁用回退 local")
    @GetMapping("/engine-status")
    public Result<ChatEngineStatusVO> engineStatus() {
        return Result.success(chatService.getEngineStatus());
    }

    @Operation(summary = "发送消息",
            description = "本地 RAG 先召回（FAQ 50 条 + 政策库 + 反诈库）→ local 模板化 / agent LLM 组织语言；"
                    + "LLM 失败自动降级 local；回答附来源可点开溯源；全程标注'模拟'")
    @PostMapping("/messages")
    public Result<ChatMessageVO> send(@Valid @RequestBody ChatRequestDTO dto) {
        Long userId = UserContext.getUserId();
        return Result.success(chatService.handle(dto, userId));
    }

    @Operation(summary = "查询历史对话（最近 20 条）")
    @GetMapping("/history")
    public Result<List<ChatMessageVO>> history() {
        Long userId = UserContext.getUserId();
        return Result.success(chatService.history(userId));
    }

    @Operation(summary = "清空历史对话")
    @DeleteMapping("/history")
    public Result<Void> clear() {
        Long userId = UserContext.getUserId();
        chatService.clear(userId);
        return Result.success();
    }
}
