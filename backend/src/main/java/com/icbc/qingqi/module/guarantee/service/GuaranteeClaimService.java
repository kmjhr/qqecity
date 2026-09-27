package com.icbc.qingqi.module.guarantee.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.icbc.qingqi.common.BizException;
import com.icbc.qingqi.common.ErrorCode;
import com.icbc.qingqi.module.guarantee.dto.*;
import com.icbc.qingqi.module.guarantee.entity.*;
import com.icbc.qingqi.module.guarantee.mapper.*;
import com.icbc.qingqi.module.message.entity.SysMessage;
import com.icbc.qingqi.module.message.mapper.SysMessageMapper;
import com.icbc.qingqi.module.user.entity.SysUser;
import com.icbc.qingqi.module.user.mapper.SysUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 保函索赔服务（G-6 索赔闭环）
 * <p>
 * 状态机：SUBMITTED → AI_REVIEW →
 *   低风险速赔 APPROVED → CLOSED
 *   存疑 → DEFENSE_PERIOD（租客申辩）→ MANUAL_REVIEW（banker 人工复核）→
 *     APPROVED（payout_amount 赔付）→ CLOSED / REJECTED（reject_reason）→ CLOSED
 * <p>
 * 全程给租客发站内信：申请收到、申辩期开始、结案通知。
 * 所有银行能力（赔付）均为模拟桩，演示数据标注"模拟"。
 */
@Slf4j
@Service
public class GuaranteeClaimService {

    private final BizGuaranteeClaimMapper claimMapper;
    private final BizGuaranteeMapper guaranteeMapper;
    private final BizGuaranteeApplicationMapper applicationMapper;
    private final BizLandlordMapper landlordMapper;
    private final SysUserMapper userMapper;
    private final SysMessageMapper messageMapper;

    // 索赔状态
    private static final String CLAIM_SUBMITTED = "SUBMITTED";
    private static final String CLAIM_AI_REVIEW = "AI_REVIEW";
    private static final String CLAIM_DEFENSE_PERIOD = "DEFENSE_PERIOD";
    private static final String CLAIM_MANUAL_REVIEW = "MANUAL_REVIEW";
    private static final String CLAIM_APPROVED = "APPROVED";
    private static final String CLAIM_REJECTED = "REJECTED";
    private static final String CLAIM_CLOSED = "CLOSED";

    // AI 初审结果
    private static final String AI_PASS = "PASS";
    private static final String AI_MANUAL_REVIEW = "MANUAL_REVIEW";

    // 保函状态
    private static final String GUARANTEE_ACTIVE = "ACTIVE";
    private static final String GUARANTEE_CLAIMED = "CLAIMED";

    // AI 初审置信度阈值（低于此值转人工）
    private static final int CONFIDENCE_THRESHOLD = 60;

    // 模拟低风险索赔金额阈值（低于此金额且证据齐全 → 速赔）
    private static final BigDecimal LOW_RISK_AMOUNT = new BigDecimal("2000");

    public GuaranteeClaimService(BizGuaranteeClaimMapper claimMapper,
                                  BizGuaranteeMapper guaranteeMapper,
                                  BizGuaranteeApplicationMapper applicationMapper,
                                  BizLandlordMapper landlordMapper,
                                  SysUserMapper userMapper,
                                  SysMessageMapper messageMapper) {
        this.claimMapper = claimMapper;
        this.guaranteeMapper = guaranteeMapper;
        this.applicationMapper = applicationMapper;
        this.landlordMapper = landlordMapper;
        this.userMapper = userMapper;
        this.messageMapper = messageMapper;
    }

    // ============================================================
    //  G-6-1 房东发起索赔
    // ============================================================

    /**
     * 房东发起索赔
     * <p>
     * 1. 校验当前用户为该保函的房东
     * 2. 校验保函状态为 ACTIVE
     * 3. 创建索赔记录（状态 SUBMITTED），自动触发 AI 初审
     * 4. 给租客发站内信"索赔申请已收到"
     */
    @Transactional(rollbackFor = Exception.class)
    public ClaimVO submitClaim(Long currentUserId, ClaimSubmitDTO dto) {
        BizGuarantee guarantee = guaranteeMapper.selectById(dto.getGuaranteeId());
        if (guarantee == null) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "保函不存在");
        }

        // 越权校验：仅本单房东可发起索赔
        BizLandlord landlord = landlordMapper.selectOne(
                new LambdaQueryWrapper<BizLandlord>().eq(BizLandlord::getUserId, currentUserId));
        if (landlord == null || !landlord.getId().equals(guarantee.getLandlordId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅该保函对应的房东可发起索赔");
        }

        // 保函状态校验
        if (!GUARANTEE_ACTIVE.equals(guarantee.getGuaranteeStatus())) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "保函状态为" + guarantee.getGuaranteeStatus() + "，不可索赔");
        }

        // 索赔金额不超过保函金额
        if (dto.getClaimAmount().compareTo(guarantee.getGuaranteeAmount()) > 0) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "索赔金额不得超过保函金额（¥" + guarantee.getGuaranteeAmount() + "）");
        }

        // 查申请人姓名
        BizGuaranteeApplication app = applicationMapper.selectById(guarantee.getApplicationId());
        String claimantName = landlord.getRealName() != null ? landlord.getRealName() : "房东";

        // 创建索赔记录
        BizGuaranteeClaim claim = new BizGuaranteeClaim();
        claim.setClaimNo(generateNo("CLM"));
        claim.setGuaranteeId(guarantee.getId());
        claim.setGuaranteeNo(guarantee.getGuaranteeNo());
        claim.setClaimantId(currentUserId);
        claim.setClaimantName(claimantName);
        claim.setTenantId(guarantee.getTenantId());
        claim.setClaimAmount(dto.getClaimAmount());
        claim.setClaimReason(dto.getClaimReason());
        claim.setEvidenceFiles(dto.getEvidenceFiles());
        claim.setClaimStatus(CLAIM_SUBMITTED);
        claim.setSubmitTime(LocalDateTime.now());
        claimMapper.insert(claim);

        log.info("[索赔提交] 索赔编号={}, 保函编号={}, 房东={}, 租客={}, 索赔金额={}",
                claim.getClaimNo(), guarantee.getGuaranteeNo(), currentUserId, guarantee.getTenantId(), dto.getClaimAmount());

        // 给租客发站内信：索赔申请已收到
        sendInternalMessage(guarantee.getTenantId(), "保函索赔通知",
                "您的房东已就保函" + guarantee.getGuaranteeNo() + "发起索赔（编号" + claim.getClaimNo()
                        + "），索赔金额¥" + dto.getClaimAmount() + "。原因：" + dto.getClaimReason() + "【模拟】",
                "GUARANTEE", claim.getId());

        // 自动触发 AI 初审
        return aiClaimReview(claim, guarantee, app);
    }

    // ============================================================
    //  G-6-2 AI 初审
    // ============================================================

    /**
     * AI 初审（模拟）
     * <p>
     * 规则：
     * 1. 证据齐全性：evidence_files 是否包含欠租记录/损坏照片/物品清单/沟通记录
     * 2. 损坏是否在交接清单已存在（模拟：证据中含"交接清单"且未标注"已存在"→ 新增损坏）
     * 3. 金额不超保函额（已在提交时校验，此处复核）
     * <p>
     * 置信度计算：
     * - 证据齐全 + 小额（≤2000）→ 置信度 85 → 低风险速赔 APPROVED
     * - 证据齐全 + 大额 → 置信度 65 → 存疑转 DEFENSE_PERIOD
     * - 证据不齐全 → 置信度 40 < 60 → 存疑转 DEFENSE_PERIOD
     */
    @Transactional(rollbackFor = Exception.class)
    public ClaimVO aiClaimReview(BizGuaranteeClaim claim, BizGuarantee guarantee, BizGuaranteeApplication app) {
        claim.setClaimStatus(CLAIM_AI_REVIEW);

        List<String> evidenceList = parseEvidence(claim.getEvidenceFiles());
        List<String> hitRules = new ArrayList<>();
        int confidence;

        // 规则1：证据齐全性
        boolean hasOverdueRecord = evidenceList.stream().anyMatch(e -> e.contains("欠租"));
        boolean hasDamagePhoto = evidenceList.stream().anyMatch(e -> e.contains("损坏") || e.contains("照片"));
        boolean hasItemList = evidenceList.stream().anyMatch(e -> e.contains("物品") || e.contains("清单"));
        boolean hasChatRecord = evidenceList.stream().anyMatch(e -> e.contains("沟通") || e.contains("聊天"));

        int evidenceCount = 0;
        if (hasOverdueRecord) evidenceCount++;
        if (hasDamagePhoto) evidenceCount++;
        if (hasItemList) evidenceCount++;
        if (hasChatRecord) evidenceCount++;

        if (evidenceCount < 2) {
            hitRules.add("证据不齐全（建议至少提供欠租记录、损坏照片等2项以上证据）");
        }

        // 规则2：损坏是否在交接清单已存在（模拟比对）
        // 演示口径：若证据中含"交接清单"且未标注"已存在"，视为新增损坏
        boolean hasTransferList = evidenceList.stream().anyMatch(e -> e.contains("交接清单"));
        if (hasItemList && !hasTransferList) {
            hitRules.add("无法比对交接清单，损坏是否为入住前已存在无法确认");
        }

        // 规则3：金额复核
        if (guarantee != null && claim.getClaimAmount().compareTo(guarantee.getGuaranteeAmount()) > 0) {
            hitRules.add("索赔金额超过保函金额");
        }

        // 置信度计算
        if (hitRules.isEmpty() && claim.getClaimAmount().compareTo(LOW_RISK_AMOUNT) <= 0) {
            // 证据齐全 + 小额 → 高置信度，速赔
            confidence = 85;
        } else if (hitRules.isEmpty()) {
            // 证据齐全但大额 → 中等置信度
            confidence = 65;
        } else {
            // 有风险命中 → 低置信度
            confidence = 40;
        }

        StringBuilder detail = new StringBuilder();
        detail.append("{\"confidence\":").append(confidence)
                .append(",\"evidenceCount\":").append(evidenceCount)
                .append(",\"hitRules\":").append(hitRules)
                .append(",\"remark\":\"AI初审（模拟）\"}");

        claim.setAiReviewResult(confidence >= CONFIDENCE_THRESHOLD && hitRules.isEmpty() ? AI_PASS : AI_MANUAL_REVIEW);
        claim.setAiReviewDetail(detail.toString());
        claim.setReviewTime(LocalDateTime.now());

        if (confidence >= CONFIDENCE_THRESHOLD && hitRules.isEmpty() && claim.getClaimAmount().compareTo(LOW_RISK_AMOUNT) <= 0) {
            // 低风险小案 → 速赔
            claim.setClaimStatus(CLAIM_APPROVED);
            claim.setPayoutAmount(claim.getClaimAmount());
            claimMapper.updateById(claim);

            // 更新保函状态为已索赔
            updateGuaranteeClaimed(guarantee);

            // 给租客发站内信：索赔已通过（速赔）
            sendInternalMessage(claim.getTenantId(), "保函索赔结案通知",
                    "保函" + claim.getGuaranteeNo() + "的索赔（编号" + claim.getClaimNo()
                            + "）经 AI 初审认定为低风险小案，已速赔通过，赔付金额¥" + claim.getPayoutAmount() + "。【模拟】",
                    "GUARANTEE", claim.getId());

            log.info("[AI初审-速赔] 索赔编号={}, 置信度={}, 赔付金额={}", claim.getClaimNo(), confidence, claim.getPayoutAmount());
        } else {
            // 存疑 → 转申辩期
            claim.setClaimStatus(CLAIM_DEFENSE_PERIOD);
            claimMapper.updateById(claim);

            // 给租客发站内信：申辩期开始
            sendInternalMessage(claim.getTenantId(), "保函索赔申辩通知",
                    "保函" + claim.getGuaranteeNo() + "的索赔（编号" + claim.getClaimNo()
                            + "）存在疑问，请您在申辩期内提交反证。索赔原因：" + claim.getClaimReason()
                            + "，索赔金额：¥" + claim.getClaimAmount() + "。【模拟】",
                    "GUARANTEE", claim.getId());

            log.info("[AI初审-转申辩] 索赔编号={}, 置信度={}, 命中规则={}", claim.getClaimNo(), confidence, hitRules);
        }

        return toClaimVO(claim);
    }

    // ============================================================
    //  G-6-3 租客提交申辩
    // ============================================================

    /**
     * 租客提交申辩
     * <p>
     * 申辩期内租客提交反证内容，提交后索赔转入人工复核 MANUAL_REVIEW
     */
    @Transactional(rollbackFor = Exception.class)
    public ClaimVO submitDefense(Long currentUserId, Long claimId, ClaimDefenseDTO dto) {
        BizGuaranteeClaim claim = getClaim(claimId);

        // 越权校验：仅被索赔租客可申辩
        if (!claim.getTenantId().equals(currentUserId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅被索赔的租客可提交申辩");
        }

        // 状态校验
        if (!CLAIM_DEFENSE_PERIOD.equals(claim.getClaimStatus())) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET,
                    "当前索赔状态为" + claimStatusName(claim.getClaimStatus()) + "，不可提交申辩");
        }

        claim.setDefenseContent(dto.getDefenseContent());
        claim.setClaimStatus(CLAIM_MANUAL_REVIEW);
        claimMapper.updateById(claim);

        log.info("[租客申辩] 索赔编号={}, 租客={}, 已转入人工复核", claim.getClaimNo(), currentUserId);

        return toClaimVO(claim);
    }

    // ============================================================
    //  G-6-4 banker 人工复核
    // ============================================================

    /**
     * banker01 人工复核索赔
     * <p>
     * 对 MANUAL_REVIEW 状态的索赔做出裁决：
     * - APPROVED：赔付（payout_amount），保函状态置 CLAIMED
     * - REJECTED：拒绝（reject_reason）
     * 裁决后状态置 CLOSED，给租客发结案通知
     */
    @Transactional(rollbackFor = Exception.class)
    public ClaimVO manualReview(Long currentUserId, Long claimId, ClaimReviewDTO dto) {
        BizGuaranteeClaim claim = getClaim(claimId);

        // 状态校验
        if (!CLAIM_MANUAL_REVIEW.equals(claim.getClaimStatus())) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET,
                    "当前索赔状态为" + claimStatusName(claim.getClaimStatus()) + "，不可人工复核");
        }

        BizGuarantee guarantee = guaranteeMapper.selectById(claim.getGuaranteeId());

        if ("APPROVED".equals(dto.getDecision())) {
            // 赔付
            if (dto.getPayoutAmount() == null || dto.getPayoutAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException(ErrorCode.PARAM_ERROR, "赔付时必须填写赔付金额");
            }
            if (guarantee != null && dto.getPayoutAmount().compareTo(guarantee.getGuaranteeAmount()) > 0) {
                throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "赔付金额不得超过保函金额");
            }
            claim.setClaimStatus(CLAIM_APPROVED);
            claim.setPayoutAmount(dto.getPayoutAmount());
            claim.setCloseTime(LocalDateTime.now());

            // 更新保函状态为已索赔
            updateGuaranteeClaimed(guarantee);

            // 给租客发结案通知
            sendInternalMessage(claim.getTenantId(), "保函索赔结案通知",
                    "保函" + claim.getGuaranteeNo() + "的索赔（编号" + claim.getClaimNo()
                            + "）经人工复核，判定赔付¥" + dto.getPayoutAmount() + "。【模拟】",
                    "GUARANTEE", claim.getId());

            log.info("[人工复核-赔付] 索赔编号={}, banker={}, 赔付金额={}", claim.getClaimNo(), currentUserId, dto.getPayoutAmount());
        } else if ("REJECTED".equals(dto.getDecision())) {
            // 拒绝
            claim.setClaimStatus(CLAIM_REJECTED);
            claim.setRejectReason(dto.getRejectReason() != null ? dto.getRejectReason() : "人工复核未通过");
            claim.setCloseTime(LocalDateTime.now());

            // 给租客发结案通知
            sendInternalMessage(claim.getTenantId(), "保函索赔结案通知",
                    "保函" + claim.getGuaranteeNo() + "的索赔（编号" + claim.getClaimNo()
                            + "）经人工复核，已拒绝。原因：" + claim.getRejectReason() + "。【模拟】",
                    "GUARANTEE", claim.getId());

            log.info("[人工复核-拒绝] 索赔编号={}, banker={}, 拒绝原因={}", claim.getClaimNo(), currentUserId, claim.getRejectReason());
        } else {
            throw new BizException(ErrorCode.PARAM_ERROR, "复核结论只能为 APPROVED 或 REJECTED");
        }

        // 索赔完结后置 CLOSED
        claim.setClaimStatus(CLAIM_CLOSED);
        claimMapper.updateById(claim);

        return toClaimVO(claim);
    }

    // ============================================================
    //  G-6-5 查询
    // ============================================================

    /**
     * 分页查询索赔列表
     * <p>
     * 房东可见自己发起的索赔，租客可见针对自己的索赔
     */
    public Page<ClaimVO> pageClaims(Long currentUserId, int pageNum, int pageSize, String status) {
        // 查找当前用户是否为房东
        BizLandlord landlord = landlordMapper.selectOne(
                new LambdaQueryWrapper<BizLandlord>().eq(BizLandlord::getUserId, currentUserId));

        Page<BizGuaranteeClaim> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BizGuaranteeClaim> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(BizGuaranteeClaim::getTenantId, currentUserId));
        if (landlord != null) {
            wrapper.or().eq(BizGuaranteeClaim::getClaimantId, currentUserId);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(BizGuaranteeClaim::getClaimStatus, status);
        }
        wrapper.orderByDesc(BizGuaranteeClaim::getCreateTime);

        Page<BizGuaranteeClaim> result = claimMapper.selectPage(page, wrapper);

        Page<ClaimVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toClaimVO).toList());
        return voPage;
    }

    /**
     * 查询索赔详情
     */
    public ClaimVO getClaimDetail(Long currentUserId, Long claimId) {
        BizGuaranteeClaim claim = getClaim(claimId);
        // 越权校验：索赔人或被索赔租客可查
        if (!claim.getClaimantId().equals(currentUserId) && !claim.getTenantId().equals(currentUserId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权查看该索赔");
        }
        return toClaimVO(claim);
    }

    /**
     * 查询人工复核队列（banker 专用）
     */
    public Page<ClaimVO> pageManualReviewQueue(int pageNum, int pageSize) {
        Page<BizGuaranteeClaim> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BizGuaranteeClaim> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizGuaranteeClaim::getClaimStatus, CLAIM_MANUAL_REVIEW);
        wrapper.orderByAsc(BizGuaranteeClaim::getCreateTime);

        Page<BizGuaranteeClaim> result = claimMapper.selectPage(page, wrapper);

        Page<ClaimVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toClaimVO).toList());
        return voPage;
    }

    // ============================================================
    //  状态流转说明
    // ============================================================

    public static Map<String, Object> claimStatusFlow() {
        List<Map<String, String>> flow = List.of(
                Map.of("status", "SUBMITTED", "name", "已提交", "desc", "房东已提交索赔申请"),
                Map.of("status", "AI_REVIEW", "name", "AI初审", "desc", "AI 初审证据齐全性与金额"),
                Map.of("status", "DEFENSE_PERIOD", "name", "申辩期", "desc", "存疑索赔，租客可提交反证"),
                Map.of("status", "MANUAL_REVIEW", "name", "人工复核", "desc", "banker 人工复核裁决"),
                Map.of("status", "APPROVED", "name", "已赔付", "desc", "索赔通过，已赔付（模拟）"),
                Map.of("status", "REJECTED", "name", "已拒绝", "desc", "索赔被拒绝"),
                Map.of("status", "CLOSED", "name", "已结案", "desc", "索赔已结案")
        );
        return Map.of("flow", flow, "remark", "演示系统，AI 初审与赔付均为模拟桩");
    }

    // ============================================================
    //  工具方法
    // ============================================================

    private BizGuaranteeClaim getClaim(Long id) {
        BizGuaranteeClaim claim = claimMapper.selectById(id);
        if (claim == null) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "索赔不存在");
        }
        return claim;
    }

    private void updateGuaranteeClaimed(BizGuarantee guarantee) {
        if (guarantee != null) {
            guarantee.setGuaranteeStatus(GUARANTEE_CLAIMED);
            guaranteeMapper.updateById(guarantee);
        }
    }

    /**
     * 解析证据 JSON 数组
     */
    private List<String> parseEvidence(String evidenceFiles) {
        if (evidenceFiles == null || evidenceFiles.isBlank()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        // 简单解析 JSON 数组 ["a","b","c"]
        String trimmed = evidenceFiles.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
            for (String item : trimmed.split(",")) {
                String cleaned = item.trim().replace("\"", "").replace("'", "");
                if (!cleaned.isEmpty()) {
                    result.add(cleaned);
                }
            }
        } else {
            result.add(trimmed);
        }
        return result;
    }

    /**
     * 发送站内信
     */
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

    private String generateNo(String prefix) {
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    /**
     * 索赔状态展示名
     */
    public static String claimStatusName(String status) {
        if (status == null) return "未知";
        return switch (status) {
            case CLAIM_SUBMITTED -> "已提交";
            case CLAIM_AI_REVIEW -> "AI初审中";
            case CLAIM_DEFENSE_PERIOD -> "申辩期";
            case CLAIM_MANUAL_REVIEW -> "人工复核中";
            case CLAIM_APPROVED -> "已赔付";
            case CLAIM_REJECTED -> "已拒绝";
            case CLAIM_CLOSED -> "已结案";
            default -> status;
        };
    }

    private ClaimVO toClaimVO(BizGuaranteeClaim claim) {
        ClaimVO vo = new ClaimVO();
        BeanUtil.copyProperties(claim, vo);
        vo.setStatusName(claimStatusName(claim.getClaimStatus()));
        return vo;
    }
}
