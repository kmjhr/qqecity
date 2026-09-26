package com.icbc.qingqi.module.message;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.icbc.qingqi.common.BizException;
import com.icbc.qingqi.common.ErrorCode;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息提醒中心（公共支撑）
 */
@Service
public class MessageService {

    private final MessageMapper messageMapper;

    public MessageService(MessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    public void add(Long userId, String msgType, String content) {
        Message msg = new Message();
        msg.setUserId(userId);
        msg.setMsgType(msgType);
        msg.setMsgContent(content);
        msg.setPushChannel("站内");
        msg.setSendTime(LocalDateTime.now());
        msg.setReadStatus(0);
        messageMapper.insert(msg);
    }

    public List<Message> listByUser(Long userId, Integer readStatus) {
        return messageMapper.selectList(
                Wrappers.<Message>lambdaQuery()
                        .eq(Message::getUserId, userId)
                        .eq(readStatus != null, Message::getReadStatus, readStatus)
                        .orderByDesc(Message::getSendTime));
    }

    public void markRead(Long userId, Long msgId) {
        Message msg = messageMapper.selectById(msgId);
        if (msg == null || !msg.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        msg.setReadStatus(1);
        messageMapper.updateById(msg);
    }
}
