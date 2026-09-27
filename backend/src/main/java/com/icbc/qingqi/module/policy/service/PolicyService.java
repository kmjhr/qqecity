package com.icbc.qingqi.module.policy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icbc.qingqi.common.BizException;
import com.icbc.qingqi.common.ErrorCode;
import com.icbc.qingqi.module.message.entity.SysMessage;
import com.icbc.qingqi.module.message.mapper.SysMessageMapper;
import com.icbc.qingqi.module.policy.dto.PolicyVO;
import com.icbc.qingqi.module.policy.entity.BizPolicy;
import com.icbc.qingqi.module.policy.mapper.BizPolicyMapper;
import com.icbc.qingqi.module.user.entity.SysUser;
import com.icbc.qingqi.module.user.mapper.SysUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 政策智能匹配服务
 * <p>
 * 缺口 #3 人才安居政策匹配 + 缺口 #4 创业贴息政策推送
 * 按 sys_user.userType 人群标签匹配 biz_policy.target_crowd
 */
@Slf4j
@Service
public class PolicyService {

    private final BizPolicyMapper policyMapper;
    private final SysUserMapper userMapper;
    private final SysMessageMapper messageMapper;

    public PolicyService(BizPolicyMapper policyMapper,
                         SysUserMapper userMapper,
                         SysMessageMapper messageMapper) {
        this.policyMapper = policyMapper;
        this.userMapper = userMapper;
        this.messageMapper = messageMapper;
    }

    /**
     * 按当前用户人群资质匹配政策
     *
     * @param userId 当前用户 ID
     * @return 匹配 + 未匹配的全部政策列表，matched=true 表示命中
     */
    public List<PolicyVO> matchPolicies(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        String userType = user.getUserType() != null ? user.getUserType() : "OTHER";

        List<BizPolicy> all = policyMapper.selectList(
                new LambdaQueryWrapper<BizPolicy>()
                        .eq(BizPolicy::getStatus, "ACTIVE")
                        .orderByAsc(BizPolicy::getSortOrder));

        return all.stream().map(p -> toVO(p, userType)).collect(Collectors.toList());
    }

    /**
     * 仅返回匹配当前用户的政策
     */
    public List<PolicyVO> matchedPolicies(Long userId) {
        return matchPolicies(userId).stream()
                .filter(PolicyVO::getMatched)
                .collect(Collectors.toList());
    }

    /**
     * 按类型筛选政策（不分人群）
     */
    public List<PolicyVO> listByType(String policyType) {
        LambdaQueryWrapper<BizPolicy> wrapper = new LambdaQueryWrapper<BizPolicy>()
                .eq(BizPolicy::getStatus, "ACTIVE")
                .orderByAsc(BizPolicy::getSortOrder);
        if (policyType != null && !policyType.isEmpty()) {
            wrapper.eq(BizPolicy::getPolicyType, policyType);
        }
        return policyMapper.selectList(wrapper).stream()
                .map(p -> toVO(p, null))
                .collect(Collectors.toList());
    }

    /**
     * 政策详情
     */
    public PolicyVO detail(Long id) {
        BizPolicy p = policyMapper.selectById(id);
        if (p == null) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "政策不存在");
        }
        return toVO(p, null);
    }

    /**
     * 智能推送：将匹配政策以站内信推送给用户
     */
    public List<PolicyVO> pushToUser(Long userId) {
        List<PolicyVO> matched = matchedPolicies(userId);
        if (matched.isEmpty()) {
            return Collections.emptyList();
        }

        SysUser user = userMapper.selectById(userId);
        String userName = user != null && user.getRealName() != null ? user.getRealName() : "用户";

        StringBuilder sb = new StringBuilder();
        sb.append("为您匹配到以下政策（模拟）：\n");
        for (PolicyVO p : matched) {
            sb.append("• ").append(p.getPolicyName());
            if (p.getMaxAmount() != null && p.getMaxAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                sb.append("（最高¥").append(p.getMaxAmount()).append("）");
            }
            sb.append("\n");
        }
        sb.append("请前往「政策匹配」页面查看详情并申报。");

        sendInternalMessage(userId, "政策智能推送", sb.toString(), "POLICY", null);

        log.info("[政策推送] 用户={}, 推送{}条匹配政策", userId, matched.size());
        return matched;
    }

    // ============================================================
    //  工具方法
    // ============================================================

    private PolicyVO toVO(BizPolicy p, String userType) {
        PolicyVO vo = new PolicyVO();
        vo.setId(p.getId());
        vo.setPolicyNo(p.getPolicyNo());
        vo.setPolicyName(p.getPolicyName());
        vo.setPolicyType(p.getPolicyType());
        vo.setPolicyTypeName("HOUSING".equals(p.getPolicyType()) ? "人才安居" : "创业贴息");
        vo.setMaxAmount(p.getMaxAmount());
        vo.setSubsidyRate(p.getSubsidyRate());
        vo.setConditions(p.getConditions());
        vo.setApplyUrl(p.getApplyUrl());
        vo.setPolicySource(p.getPolicySource());
        vo.setSortOrder(p.getSortOrder());

        List<String> crowdList = Arrays.asList(p.getTargetCrowd().split(","));
        vo.setTargetCrowdList(crowdList);

        if (userType != null) {
            boolean matched = crowdList.contains(userType);
            vo.setMatched(matched);
            vo.setMatchReason(matched
                    ? "您的人群类型「" + crowdTypeName(userType) + "」符合该政策申报条件"
                    : "该政策适用人群：" + crowdList.stream().map(this::crowdTypeName).collect(Collectors.joining("、")));
        } else {
            vo.setMatched(false);
        }
        return vo;
    }

    private String crowdTypeName(String type) {
        return switch (type) {
            case "STUDENT" -> "在校生";
            case "GRADUATE" -> "毕业2年内";
            case "ENTREPRENEUR" -> "青年创业者";
            case "OTHER" -> "其他青年";
            default -> type;
        };
    }

    private void sendInternalMessage(Long userId, String title, String content, String bizType, Long bizId) {
        SysMessage msg = new SysMessage();
        msg.setUserId(userId);
        msg.setTitle(title);
        msg.setContent(content);
        msg.setType("BUSINESS");
        msg.setBizType(bizType);
        msg.setBizId(bizId);
        msg.setIsRead(0);
        messageMapper.insert(msg);
    }
}
