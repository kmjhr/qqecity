package com.icbc.qingqi.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.icbc.qingqi.module.user.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 * <p>
 * 继承 BaseMapper 即获得基础 CRUD 能力
 * 复杂查询在 resources/mapper/ 下写 XML
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
