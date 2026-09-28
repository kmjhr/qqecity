package com.icbc.qingqi.module.policy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icbc.qingqi.module.policy.dto.PolicyPortalVO;
import com.icbc.qingqi.module.policy.entity.BizPolicyPortal;
import com.icbc.qingqi.module.policy.mapper.BizPolicyPortalMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 官方政策入口导航服务
 * <p>
 * 汇总全国/各省人社、住建房管、政务、税务等官方入口，支持按地区/类型筛选
 */
@Service
public class PolicyPortalService {

    private static final Map<String, String> TYPE_NAMES = Map.of(
            "GOV_HR", "人社",
            "GOV_HOUSING", "住建房管",
            "GOV_AFFAIR", "政务服务",
            "GOV_TAX", "税务",
            "GOV_EDU", "教育高校",
            "GOV_OTHER", "其他");

    private final BizPolicyPortalMapper portalMapper;

    public PolicyPortalService(BizPolicyPortalMapper portalMapper) {
        this.portalMapper = portalMapper;
    }

    /**
     * 官方入口列表（按地区/类型筛选）
     *
     * @param region     地区（全国/浙江/广东…），空=全部
     * @param portalType 入口类型（GOV_HR/GOV_HOUSING/GOV_AFFAIR/GOV_TAX/GOV_EDU），空=全部
     */
    public List<PolicyPortalVO> list(String region, String portalType) {
        LambdaQueryWrapper<BizPolicyPortal> wrapper = new LambdaQueryWrapper<BizPolicyPortal>()
                .eq(BizPolicyPortal::getStatus, "ACTIVE")
                .orderByAsc(BizPolicyPortal::getSortOrder);
        if (region != null && !region.isBlank()) {
            wrapper.eq(BizPolicyPortal::getRegion, region);
        }
        if (portalType != null && !portalType.isBlank()) {
            wrapper.eq(BizPolicyPortal::getPortalType, portalType);
        }
        return portalMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /** 地区列表（官方入口覆盖的地区） */
    public List<String> regions() {
        return portalMapper.selectList(new LambdaQueryWrapper<BizPolicyPortal>()
                        .eq(BizPolicyPortal::getStatus, "ACTIVE")
                        .select(BizPolicyPortal::getRegion))
                .stream()
                .map(BizPolicyPortal::getRegion)
                .distinct()
                .collect(Collectors.toList());
    }

    private PolicyPortalVO toVO(BizPolicyPortal p) {
        PolicyPortalVO vo = new PolicyPortalVO();
        vo.setId(p.getId());
        vo.setPortalName(p.getPortalName());
        vo.setPortalType(p.getPortalType());
        vo.setPortalTypeName(TYPE_NAMES.getOrDefault(p.getPortalType(), p.getPortalType()));
        vo.setRegion(p.getRegion());
        vo.setUrl(p.getUrl());
        vo.setDescription(p.getDescription());
        vo.setSortOrder(p.getSortOrder());
        return vo;
    }
}
