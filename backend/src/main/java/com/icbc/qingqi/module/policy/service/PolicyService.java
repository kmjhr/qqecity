package com.icbc.qingqi.module.policy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icbc.qingqi.common.BizException;
import com.icbc.qingqi.common.ErrorCode;
import com.icbc.qingqi.module.loan.entity.BizLoanApplication;
import com.icbc.qingqi.module.loan.mapper.BizLoanApplicationMapper;
import com.icbc.qingqi.module.message.entity.SysMessage;
import com.icbc.qingqi.module.message.mapper.SysMessageMapper;
import com.icbc.qingqi.module.policy.dto.PolicyVO;
import com.icbc.qingqi.module.policy.entity.BizPolicy;
import com.icbc.qingqi.module.policy.mapper.BizPolicyMapper;
import com.icbc.qingqi.module.user.entity.SysUser;
import com.icbc.qingqi.module.user.mapper.SysUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 政策智能匹配服务（升级版）
 * <p>
 * - 政策库 12 条杭州真实政策，按有效期过滤（valid_to 过期自动不展示）
 * - 关键词匹配：人群资质 + 创业行为特征，与 policy.keyword_tags 命中即匹配
 * - 自动推送：匹配结果自动写入消息中心（去重，手动按钮可强推）
 */
@Slf4j
@Service
public class PolicyService {

    private final BizPolicyMapper policyMapper;
    private final SysUserMapper userMapper;
    private final SysMessageMapper messageMapper;
    private final BizLoanApplicationMapper applicationMapper;

    public PolicyService(BizPolicyMapper policyMapper,
                         SysUserMapper userMapper,
                         SysMessageMapper messageMapper,
                         BizLoanApplicationMapper applicationMapper) {
        this.policyMapper = policyMapper;
        this.userMapper = userMapper;
        this.messageMapper = messageMapper;
        this.applicationMapper = applicationMapper;
    }

    /** 人群资质 → 匹配关键词 */
    private static final Map<String, String> CROWD_KEYWORDS = Map.of(
            "STUDENT", "在校生",
            "GRADUATE", "毕业生",
            "ENTREPRENEUR", "创业者",
            "VETERAN", "退役军人",
            "DISABLED", "残疾人",
            "FARMER", "农民工",
            "OTHER", "其他青年");

    /** 有贷款/预审记录即视为创业行为 */
    private static final String[] LOAN_CROWD_KEYS = {"STUDENT", "GRADUATE", "ENTREPRENEUR", "VETERAN", "DISABLED", "FARMER"};

    /**
     * 按当前用户匹配政策（自动过滤过期政策；自动推送匹配结果到消息中心）
     */
    public List<PolicyVO> matchPolicies(Long userId, String region) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        List<String> userKeywords = buildUserKeywords(userId, user);
        List<PolicyVO> vos = listActivePolicies(region).stream()
                .map(p -> toVO(p, userKeywords))
                .collect(Collectors.toList());
        autoPush(userId, user, vos);
        return vos;
    }

    /**
     * 仅返回匹配当前用户的政策
     */
    public List<PolicyVO> matchedPolicies(Long userId, String region) {
        return matchPolicies(userId, region).stream()
                .filter(PolicyVO::getMatched)
                .collect(Collectors.toList());
    }

    /**
     * 按类型筛选政策（只返回当前有效期内的政策）
     */
    public List<PolicyVO> listByType(String policyType, String region) {
        List<BizPolicy> list = listActivePolicies(region);
        if (policyType != null && !policyType.isEmpty()) {
            list = list.stream()
                    .filter(p -> policyType.equals(p.getPolicyType()))
                    .collect(Collectors.toList());
        }
        return list.stream().map(p -> toVO(p, null)).collect(Collectors.toList());
    }

    /**
     * 政策详情（不受有效期过滤影响，但标注状态）
     */
    public PolicyVO detail(Long id) {
        BizPolicy p = policyMapper.selectById(id);
        if (p == null) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "政策不存在");
        }
        return toVO(p, null);
    }

    /**
     * 手动推送：将匹配政策以站内信推送给用户（先清理旧的 POLICY 推送再推，保证最新）
     */
    public List<PolicyVO> pushToUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        // 清理旧的 POLICY 站内信，避免重复
        messageMapper.delete(new LambdaQueryWrapper<SysMessage>()
                .eq(SysMessage::getUserId, userId)
                .eq(SysMessage::getBizType, "POLICY"));
        List<String> userKeywords = buildUserKeywords(userId, user);
        List<PolicyVO> matched = listActivePolicies(null).stream()
                .map(p -> toVO(p, userKeywords))
                .filter(PolicyVO::getMatched)
                .collect(Collectors.toList());
        if (matched.isEmpty()) {
            return Collections.emptyList();
        }
        pushMessage(user, matched);
        log.info("[政策推送-手动] 用户={}, 推送{}条匹配政策", userId, matched.size());
        return matched;
    }

    // ============================================================
    //  工具方法
    // ============================================================

    /** 有效期内政策（status=ACTIVE 且 valid_to 未过期） */
    private List<BizPolicy> listActivePolicies(String region) {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<BizPolicy> wrapper = new LambdaQueryWrapper<BizPolicy>()
                .eq(BizPolicy::getStatus, "ACTIVE")
                .and(w -> w.isNull(BizPolicy::getValidTo).or().ge(BizPolicy::getValidTo, today));
        if (region != null && !region.isBlank()) {
            wrapper.eq(BizPolicy::getRegion, region);
        }
        return policyMapper.selectList(wrapper.orderByAsc(BizPolicy::getSortOrder));
    }

    /** 构建用户匹配关键词：人群资质 + 创业行为 */
    private List<String> buildUserKeywords(Long userId, SysUser user) {
        Set<String> kws = new LinkedHashSet<>();
        String userType = user.getUserType() != null ? user.getUserType() : "OTHER";
        if (CROWD_KEYWORDS.containsKey(userType)) {
            kws.add(CROWD_KEYWORDS.get(userType));
        }
        // 毕业生/在校生补充关键词
        if ("GRADUATE".equals(userType) || "STUDENT".equals(userType)) {
            kws.add("毕业生");
            kws.add("在校生");
        }
        // 有贷款申请/预审记录 → 视为创业行为
        boolean hasLoan = Arrays.asList(LOAN_CROWD_KEYS).contains(userType)
                && applicationMapper.selectCount(new LambdaQueryWrapper<BizLoanApplication>()
                .eq(BizLoanApplication::getUserId, userId)) > 0;
        if (hasLoan) {
            kws.add("创业");
        }
        return new ArrayList<>(kws);
    }

    private PolicyVO toVO(BizPolicy p, List<String> userKeywords) {
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
        vo.setValidFrom(p.getValidFrom());
        vo.setValidTo(p.getValidTo());
        vo.setKeywordTags(p.getKeywordTags());
        vo.setRegion(p.getRegion());
        vo.setPolicySummary(p.getPolicySummary());
        vo.setSortOrder(p.getSortOrder());

        // 有效期状态
        boolean expired = p.getValidTo() != null && p.getValidTo().isBefore(LocalDate.now());
        vo.setValidityStatus(expired ? "EXPIRED" : "VALID");
        vo.setValidityStatusName(expired ? "已过期" : "有效期内");

        List<String> crowdList = Arrays.asList(p.getTargetCrowd().split(","));
        vo.setTargetCrowdList(crowdList);

        if (userKeywords != null) {
            List<String> tags = p.getKeywordTags() == null || p.getKeywordTags().isBlank()
                    ? crowdList : Arrays.asList(p.getKeywordTags().split(","));
            List<String> hit = tags.stream()
                    .map(String::trim)
                    .filter(t -> userKeywords.contains(t))
                    .collect(Collectors.toList());
            vo.setMatched(!hit.isEmpty());
            vo.setHitKeywords(hit);
            vo.setMatchReason(hit.isEmpty()
                    ? "该政策需满足：" + tags.stream().map(String::trim).collect(Collectors.joining("、"))
                    : "您符合申报条件：命中关键词「" + String.join("、", hit) + "」");
        } else {
            vo.setMatched(false);
            vo.setHitKeywords(Collections.emptyList());
        }
        return vo;
    }

    /** 自动推送：匹配政策自动发消息中心（同用户已有 POLICY 推送则跳过） */
    private void autoPush(Long userId, SysUser user, List<PolicyVO> vos) {
        List<PolicyVO> matched = vos.stream().filter(PolicyVO::getMatched).collect(Collectors.toList());
        if (matched.isEmpty()) {
            return;
        }
        Long exist = messageMapper.selectCount(new LambdaQueryWrapper<SysMessage>()
                .eq(SysMessage::getUserId, userId)
                .eq(SysMessage::getBizType, "POLICY"));
        if (exist != null && exist > 0) {
            return;
        }
        pushMessage(user, matched);
        log.info("[政策推送-自动] 用户={}, 自动推送{}条匹配政策", userId, matched.size());
    }

    private void pushMessage(SysUser user, List<PolicyVO> matched) {
        StringBuilder sb = new StringBuilder();
        sb.append("为您自动匹配到 ").append(matched.size()).append(" 条在有效期内的政策（模拟推送）：\n");
        List<PolicyVO> top = matched.stream().limit(5).collect(Collectors.toList());
        for (PolicyVO p : top) {
            sb.append("• ").append(p.getPolicyName()).append("（").append(p.getRegion()).append("）");
            if (p.getMaxAmount() != null && p.getMaxAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                sb.append(" 最高¥").append(p.getMaxAmount());
            }
            sb.append("\n");
        }
        if (matched.size() > 5) {
            sb.append("…等共 ").append(matched.size()).append(" 条\n");
        }
        sb.append("点击「详情/去申报」可跳转官方申报入口。");

        SysMessage msg = new SysMessage();
        msg.setUserId(user.getId());
        msg.setTitle("政策智能推送（自动匹配）");
        msg.setContent(sb.toString());
        msg.setType("BUSINESS");
        msg.setBizType("POLICY");
        msg.setIsRead(0);
        messageMapper.insert(msg);
    }
}
