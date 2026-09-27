package com.icbc.qingqi.module.consumption.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icbc.qingqi.common.BizException;
import com.icbc.qingqi.common.ErrorCode;
import com.icbc.qingqi.module.consumption.dto.FinanceProductVO;
import com.icbc.qingqi.module.consumption.dto.RiskAssessmentVO;
import com.icbc.qingqi.module.consumption.entity.BizFinanceProduct;
import com.icbc.qingqi.module.consumption.mapper.BizFinanceProductMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 理财产品匹配与推荐服务（仅低风险，模拟）
 * <p>
 * 缺口 #7 理财匹配与风险测评（产品池部分）
 * 合规口径：理财非存款、产品有风险、工行仅代销
 * 强制测评：推荐前必须完成有效测评
 * 产品池：仅 R1/R2 低风险产品（5 条演示数据）
 */
@Slf4j
@Service
public class FinanceProductService {

    private static final String COMPLIANCE_NOTICE =
            "理财非存款、产品有风险、工行仅代销。本演示为模拟产品，不构成投资建议。";

    private final BizFinanceProductMapper productMapper;
    private final RiskAssessmentService assessmentService;

    public FinanceProductService(BizFinanceProductMapper productMapper,
                                 RiskAssessmentService assessmentService) {
        this.productMapper = productMapper;
        this.assessmentService = assessmentService;
    }

    /**
     * 列出全部产品（仅展示，不强制测评）
     */
    public List<FinanceProductVO> listAll() {
        return productMapper.selectList(
                new LambdaQueryWrapper<BizFinanceProduct>()
                        .eq(BizFinanceProduct::getStatus, "ACTIVE")
                        .orderByAsc(BizFinanceProduct::getSortOrder))
                .stream().map(p -> toVO(p, null)).collect(Collectors.toList());
    }

    /**
     * 按当前用户风险等级推荐产品
     * <p>
     * 强制测评：未完成有效测评返回 1001
     */
    public List<FinanceProductVO> recommendByUser(Long userId) {
        if (!assessmentService.hasValidAssessment(userId)) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "尚未完成风险测评或测评已过期，请先完成测评后再查看产品推荐");
        }
        RiskAssessmentVO vo = assessmentService.latest(userId);
        return recommendByLevel(vo.getRiskLevel());
    }

    /**
     * 按指定风险等级推荐产品
     */
    public List<FinanceProductVO> recommendByLevel(String riskLevel) {
        if (riskLevel == null || riskLevel.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "风险等级不能为空");
        }
        List<BizFinanceProduct> all = productMapper.selectList(
                new LambdaQueryWrapper<BizFinanceProduct>()
                        .eq(BizFinanceProduct::getStatus, "ACTIVE")
                        .orderByAsc(BizFinanceProduct::getSortOrder));
        return all.stream()
                .map(p -> toVO(p, riskLevel))
                .collect(Collectors.toList());
    }

    public FinanceProductVO detail(Long id) {
        BizFinanceProduct p = productMapper.selectById(id);
        if (p == null) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "产品不存在");
        }
        return toVO(p, null);
    }

    /**
     * 购买演示（返回模拟链接 + 风险提示）
     * <p>
     * 强制测评 + 产品适配校验 + 不发起真实扣款
     */
    public ApplyResult applyDemo(Long userId, Long productId) {
        if (!assessmentService.hasValidAssessment(userId)) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "尚未完成风险测评或测评已过期，请先完成测评后再购买");
        }

        BizFinanceProduct p = productMapper.selectById(productId);
        if (p == null) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "产品不存在");
        }
        if (!"ACTIVE".equals(p.getStatus())) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "产品已下架");
        }

        RiskAssessmentVO vo = assessmentService.latest(userId);
        List<String> targetLevels = Arrays.asList(p.getTargetRiskLevel().split(","));
        if (!targetLevels.contains(vo.getRiskLevel())) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET,
                    "该产品适配 " + p.getTargetRiskLevel() + " 风险等级用户，您当前为「"
                            + vo.getRiskLevelName() + "」，不适合购买此产品");
        }

        ApplyResult result = new ApplyResult();
        result.setProductId(p.getId());
        result.setProductCode(p.getProductCode());
        result.setProductName(p.getProductName());
        result.setRiskLevel(p.getRiskLevel());
        result.setRiskDisclosure(p.getRiskDisclosure());
        result.setApplyUrl(p.getApplyUrl());
        result.setApplyStatus("DEMO_LINK_GENERATED");
        result.setUserRiskLevel(vo.getRiskLevel());
        result.setUserRiskLevelName(vo.getRiskLevelName());
        result.setComplianceNotice(COMPLIANCE_NOTICE);
        result.setNotice("模拟演示：未发起真实购买，未扣款，未向产品方传输任何用户信息");

        log.info("[理财购买-演示] 用户={}, 产品={}, 用户等级={}",
                userId, p.getProductCode(), vo.getRiskLevel());
        return result;
    }

    // ============================================================
    //  工具
    // ============================================================

    private FinanceProductVO toVO(BizFinanceProduct p, String userRiskLevel) {
        FinanceProductVO vo = new FinanceProductVO();
        vo.setId(p.getId());
        vo.setProductCode(p.getProductCode());
        vo.setProductName(p.getProductName());
        vo.setProductType(p.getProductType());
        vo.setProductTypeName(productTypeName(p.getProductType()));
        vo.setRiskLevel(p.getRiskLevel());
        vo.setRiskLevelName("R1".equals(p.getRiskLevel()) ? "低风险" : "中低风险");
        vo.setExpectedReturn(p.getExpectedReturn());
        vo.setMinAmount(p.getMinAmount());
        vo.setPeriod(p.getPeriod());
        vo.setProductElements(p.getProductElements());
        vo.setRiskDisclosure(p.getRiskDisclosure());
        vo.setApplyUrl(p.getApplyUrl());
        vo.setTargetRiskLevel(p.getTargetRiskLevel());
        vo.setSortOrder(p.getSortOrder());
        vo.setComplianceNotice(COMPLIANCE_NOTICE);

        if (userRiskLevel != null) {
            List<String> targetLevels = Arrays.asList(p.getTargetRiskLevel().split(","));
            vo.setMatched(targetLevels.contains(userRiskLevel));
        } else {
            vo.setMatched(false);
        }
        return vo;
    }

    private String productTypeName(String type) {
        return switch (type) {
            case "SAVING_GOAL" -> "心愿储蓄";
            case "CASH_MANAGEMENT" -> "现金管理类";
            case "SHORT_BOND" -> "短债基金";
            case "FUND_DCA" -> "基金定投";
            case "GOLD_ACCUM" -> "积存金";
            default -> type;
        };
    }

    @Data
    public static class ApplyResult {
        private Long productId;
        private String productCode;
        private String productName;
        private String riskLevel;
        private String riskDisclosure;
        private String applyUrl;
        private String applyStatus;
        private String userRiskLevel;
        private String userRiskLevelName;
        private String complianceNotice;
        private String notice;
    }
}
