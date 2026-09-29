package com.icbc.qingqi.module.guarantee.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.icbc.qingqi.common.BizException;
import com.icbc.qingqi.common.ErrorCode;
import com.icbc.qingqi.module.guarantee.dto.LandlordUpdateDTO;
import com.icbc.qingqi.module.guarantee.dto.LandlordVO;
import com.icbc.qingqi.module.guarantee.entity.BizHouse;
import com.icbc.qingqi.module.guarantee.entity.BizLandlord;
import com.icbc.qingqi.module.guarantee.mapper.BizHouseMapper;
import com.icbc.qingqi.module.guarantee.mapper.BizLandlordMapper;
import com.icbc.qingqi.module.user.entity.SysUser;
import com.icbc.qingqi.module.user.mapper.SysUserMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端 - 房东管理服务
 * <p>
 * 房东为独立业务主体（biz_landlord，类似商户而非普通用户），
 * 提供管理端"房东分区"：分页查询、详情、编辑、认证裁决、删除。
 */
@Service
public class LandlordAdminService {

    private final BizLandlordMapper landlordMapper;
    private final SysUserMapper userMapper;
    private final BizHouseMapper houseMapper;

    public LandlordAdminService(BizLandlordMapper landlordMapper,
                                SysUserMapper userMapper,
                                BizHouseMapper houseMapper) {
        this.landlordMapper = landlordMapper;
        this.userMapper = userMapper;
        this.houseMapper = houseMapper;
    }

    /** 分页查询房东列表（keyword：姓名/电话；verifyStatus：认证状态筛选） */
    public Page<LandlordVO> page(int pageNum, int pageSize, String keyword, String verifyStatus) {
        Page<BizLandlord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BizLandlord> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            w.and(x -> x.like(BizLandlord::getRealName, keyword)
                    .or().like(BizLandlord::getPhone, keyword)
                    .or().like(BizLandlord::getIdCard, keyword));
        }
        if (verifyStatus != null && !verifyStatus.isEmpty()) {
            w.eq(BizLandlord::getVerifyStatus, verifyStatus);
        }
        w.orderByDesc(BizLandlord::getCreateTime);
        Page<BizLandlord> result = landlordMapper.selectPage(page, w);

        Page<LandlordVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        if (result.getRecords().isEmpty()) {
            return voPage;
        }
        // 批量取关联用户与房屋数
        var userIds = result.getRecords().stream()
                .map(BizLandlord::getUserId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, SysUser> userMap = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(SysUser::getId, u -> u));
        var landlordIds = result.getRecords().stream().map(BizLandlord::getId).collect(Collectors.toList());
        Map<Long, Long> houseCountMap = houseMapper.selectList(
                        new LambdaQueryWrapper<BizHouse>().in(BizHouse::getLandlordId, landlordIds))
                .stream().collect(Collectors.groupingBy(BizHouse::getLandlordId, Collectors.counting()));

        voPage.setRecords(result.getRecords().stream().map(l -> toVO(l, userMap.get(l.getUserId()),
                houseCountMap.getOrDefault(l.getId(), 0L).intValue())).toList());
        return voPage;
    }

    /** 房东详情 */
    public LandlordVO getById(Long id) {
        BizLandlord landlord = landlordMapper.selectById(id);
        if (landlord == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "房东不存在");
        }
        SysUser user = landlord.getUserId() != null ? userMapper.selectById(landlord.getUserId()) : null;
        Long cnt = houseMapper.selectCount(new LambdaQueryWrapper<BizHouse>().eq(BizHouse::getLandlordId, id));
        return toVO(landlord, user, cnt == null ? 0 : cnt.intValue());
    }

    /** 编辑房东信息 */
    public void update(Long id, LandlordUpdateDTO dto) {
        BizLandlord landlord = landlordMapper.selectById(id);
        if (landlord == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "房东不存在");
        }
        BizLandlord update = new BizLandlord();
        update.setId(id);
        if (dto.getRealName() != null) update.setRealName(dto.getRealName());
        if (dto.getIdCard() != null) update.setIdCard(dto.getIdCard());
        if (dto.getPhone() != null) update.setPhone(dto.getPhone());
        if (dto.getBankAccount() != null) update.setBankAccount(dto.getBankAccount());
        if (dto.getBankName() != null) update.setBankName(dto.getBankName());
        landlordMapper.updateById(update);
    }

    /** 认证裁决：VERIFIED-通过 / REJECTED-驳回 */
    public void verify(Long id, String verifyStatus) {
        if (verifyStatus == null || !("VERIFIED".equals(verifyStatus) || "REJECTED".equals(verifyStatus)
                || "PENDING".equals(verifyStatus))) {
            throw new BizException(ErrorCode.PARAM_ERROR);
        }
        BizLandlord landlord = landlordMapper.selectById(id);
        if (landlord == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "房东不存在");
        }
        BizLandlord update = new BizLandlord();
        update.setId(id);
        update.setVerifyStatus(verifyStatus);
        landlordMapper.updateById(update);
    }

    /** 删除房东：逻辑删除房东记录 + 停用关联系统用户 */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        BizLandlord landlord = landlordMapper.selectById(id);
        if (landlord == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "房东不存在");
        }
        landlordMapper.deleteById(id);
        if (landlord.getUserId() != null) {
            SysUser user = new SysUser();
            user.setId(landlord.getUserId());
            user.setStatus(0);
            userMapper.updateById(user);
        }
    }

    private LandlordVO toVO(BizLandlord l, SysUser user, int houseCount) {
        LandlordVO vo = new LandlordVO();
        BeanUtils.copyProperties(l, vo);
        vo.setHouseCount(houseCount);
        vo.setUsername(user != null ? user.getUsername() : null);
        vo.setStatus(user != null ? user.getStatus() : 0);
        return vo;
    }
}
