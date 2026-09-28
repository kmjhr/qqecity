package com.icbc.qingqi.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.icbc.qingqi.module.user.entity.BizSchool;
import org.apache.ibatis.annotations.Mapper;

/**
 * 高校库 Mapper
 * <p>
 * 对应表：biz_school（注册学历核验白名单，模拟）
 */
@Mapper
public interface BizSchoolMapper extends BaseMapper<BizSchool> {
}
