package com.icbc.qingqi.module.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.icbc.qingqi.module.message.entity.SysMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息 Mapper
 * <p>
 * 继承 BaseMapper 获得基础 CRUD 能力
 */
@Mapper
public interface SysMessageMapper extends BaseMapper<SysMessage> {
}
