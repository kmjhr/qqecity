package com.icbc.qingqi.module.message.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.module.message.dto.MessageVO;
import com.icbc.qingqi.module.message.service.SysMessageService;
import com.icbc.qingqi.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 消息中心接口
 * <p>
 * 路径：/api/v1/message/** （需要登录）
 * 供用户端网页、微信小程序调用
 * <p>
 * 公共支撑模块：消息中心
 * 提供消息列表查询、标记已读、未读数量等能力
 */
@Tag(name = "消息中心")
@RestController
@RequestMapping("/v1/message")
public class MessageController {

    private final SysMessageService messageService;

    public MessageController(SysMessageService messageService) {
        this.messageService = messageService;
    }

    @Operation(summary = "分页查询当前用户消息列表")
    @GetMapping("/page")
    public Result<Page<MessageVO>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer isRead) {
        Long userId = UserContext.getUserId();
        return Result.success(messageService.pageMessages(userId, pageNum, pageSize, type, isRead));
    }

    @Operation(summary = "标记单条消息已读")
    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        messageService.markAsRead(userId, id);
        return Result.success();
    }

    @Operation(summary = "全部标记已读")
    @PutMapping("/read-all")
    public Result<Void> markAllAsRead() {
        Long userId = UserContext.getUserId();
        messageService.markAllAsRead(userId);
        return Result.success();
    }

    @Operation(summary = "获取未读消息数量")
    @GetMapping("/unread-count")
    public Result<Map<String, Long>> unreadCount() {
        Long userId = UserContext.getUserId();
        Long count = messageService.getUnreadCount(userId);
        Map<String, Long> result = new HashMap<>();
        result.put("unreadCount", count);
        return Result.success(result);
    }
}
