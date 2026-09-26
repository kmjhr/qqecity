package com.icbc.qingqi.module.message;

import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.security.UserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 消息提醒中心接口
 */
@RestController
@RequestMapping("/api/v1/message")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/list")
    public Result<List<Message>> list(@RequestParam(required = false) Integer readStatus) {
        return Result.ok(messageService.listByUser(UserContext.requireUserId(), readStatus));
    }

    @PostMapping("/{msgId}/read")
    public Result<Void> read(@PathVariable Long msgId) {
        messageService.markRead(UserContext.requireUserId(), msgId);
        return Result.ok();
    }
}
