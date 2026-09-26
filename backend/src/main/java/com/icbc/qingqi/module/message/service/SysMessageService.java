package com.icbc.qingqi.module.message.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.icbc.qingqi.common.BizException;
import com.icbc.qingqi.common.ErrorCode;
import com.icbc.qingqi.module.message.dto.MessageVO;
import com.icbc.qingqi.module.message.entity.SysMessage;
import com.icbc.qingqi.module.message.mapper.SysMessageMapper;
import org.springframework.stereotype.Service;

/**
 * 消息中心服务
 * <p>
 * 提供消息分页查询、标记已读、未读数量查询等能力
 * 所有操作均基于当前登录用户，确保数据隔离
 */
@Service
public class SysMessageService {

    private final SysMessageMapper messageMapper;

    public SysMessageService(SysMessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    /**
     * 分页查询当前用户消息列表
     *
     * @param userId   当前用户 ID
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @param type     消息类型（可选，不传查询全部）
     * @param isRead   是否已读（可选，不传查询全部）
     */
    public Page<MessageVO> pageMessages(Long userId, int pageNum, int pageSize,
                                        String type, Integer isRead) {
        Page<SysMessage> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMessage::getUserId, userId);
        if (type != null && !type.isEmpty()) {
            wrapper.eq(SysMessage::getType, type);
        }
        if (isRead != null) {
            wrapper.eq(SysMessage::getIsRead, isRead);
        }
        wrapper.orderByDesc(SysMessage::getCreateTime);

        Page<SysMessage> result = messageMapper.selectPage(page, wrapper);

        // 转换为 VO
        Page<MessageVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    /**
     * 标记单条消息为已读
     *
     * @param userId 当前用户 ID
     * @param id     消息 ID
     */
    public void markAsRead(Long userId, Long id) {
        SysMessage message = messageMapper.selectById(id);
        if (message == null || !message.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "消息不存在或无权操作");
        }
        if (message.getIsRead() != null && message.getIsRead() == 1) {
            return; // 已读则不重复更新
        }
        SysMessage update = new SysMessage();
        update.setId(id);
        update.setIsRead(1);
        messageMapper.updateById(update);
    }

    /**
     * 全部标记已读
     *
     * @param userId 当前用户 ID
     */
    public void markAllAsRead(Long userId) {
        LambdaUpdateWrapper<SysMessage> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysMessage::getUserId, userId)
                .eq(SysMessage::getIsRead, 0)
                .set(SysMessage::getIsRead, 1);
        messageMapper.update(null, wrapper);
    }

    /**
     * 获取未读消息数量
     *
     * @param userId 当前用户 ID
     * @return 未读数量
     */
    public Long getUnreadCount(Long userId) {
        Long count = messageMapper.selectCount(
                new LambdaQueryWrapper<SysMessage>()
                        .eq(SysMessage::getUserId, userId)
                        .eq(SysMessage::getIsRead, 0));
        return count != null ? count : 0L;
    }

    // ============================================================
    //  工具方法
    // ============================================================

    private MessageVO toVO(SysMessage message) {
        MessageVO vo = new MessageVO();
        BeanUtil.copyProperties(message, vo);
        return vo;
    }
}
