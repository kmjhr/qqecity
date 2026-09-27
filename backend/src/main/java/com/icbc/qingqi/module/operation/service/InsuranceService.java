package com.icbc.qingqi.module.operation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icbc.qingqi.common.BizException;
import com.icbc.qingqi.common.ErrorCode;
import com.icbc.qingqi.module.operation.dto.InsuranceProductVO;
import com.icbc.qingqi.module.operation.entity.BizInsuranceProduct;
import com.icbc.qingqi.module.operation.mapper.BizInsuranceProductMapper;
import com.icbc.qingqi.module.user.entity.SysUser;
import com.icbc.qingqi.module.user.mapper.SysUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 保险代销匹配服务（模拟）
 * <p>
 * 缺口 #24 保险代销匹配
 * 履约保证保险 / 知识产权保险 / 财产综合险 按经营场景推荐
 * 合规口径：工行仅代销、不承保；演示用，不构成真实投保邀约
 */
@Slf4j
@Service
public class InsuranceService {

    /** 合规声明（所有产品固定展示） */
    private static final String COMPLIANCE_NOTICE =
            "本产品为模拟演示，工行仅作为代销渠道，不承担承保责任。实际投保请以保险机构正式条款为准。";

    private final BizInsuranceProductMapper productMapper;
    private final SysUserMapper userMapper;

    public InsuranceService(BizInsuranceProductMapper productMapper,
                            SysUserMapper userMapper) {
        this.productMapper = productMapper;
        this.userMapper = userMapper;
    }

    /**
     * 按当前用户人群资质匹配保险产品
     *
     * @param userId 当前用户 ID
     * @return 全部产品列表，matched=true 表示命中
     */
    public List<InsuranceProductVO> matchProducts(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        String userType = user.getUserType() != null ? user.getUserType() : "OTHER";

        List<BizInsuranceProduct> all = productMapper.selectList(
                new LambdaQueryWrapper<BizInsuranceProduct>()
                        .eq(BizInsuranceProduct::getStatus, "ACTIVE")
                        .orderByAsc(BizInsuranceProduct::getSortOrder));

        return all.stream().map(p -> toVO(p, userType)).collect(Collectors.toList());
    }

    /**
     * 仅返回匹配当前用户的产品
     */
    public List<InsuranceProductVO> matchedProducts(Long userId) {
        return matchProducts(userId).stream()
                .filter(InsuranceProductVO::getMatched)
                .collect(Collectors.toList());
    }

    /**
     * 按险种或场景筛选产品（不分人群）
     */
    public List<InsuranceProductVO> listByFilter(String insuranceType, String scene) {
        LambdaQueryWrapper<BizInsuranceProduct> wrapper = new LambdaQueryWrapper<BizInsuranceProduct>()
                .eq(BizInsuranceProduct::getStatus, "ACTIVE")
                .orderByAsc(BizInsuranceProduct::getSortOrder);
        if (insuranceType != null && !insuranceType.isEmpty()) {
            wrapper.eq(BizInsuranceProduct::getInsuranceType, insuranceType);
        }
        if (scene != null && !scene.isEmpty()) {
            wrapper.eq(BizInsuranceProduct::getScene, scene);
        }
        return productMapper.selectList(wrapper).stream()
                .map(p -> toVO(p, null))
                .collect(Collectors.toList());
    }

    /**
     * 产品详情
     */
    public InsuranceProductVO detail(Long id) {
        BizInsuranceProduct p = productMapper.selectById(id);
        if (p == null) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "保险产品不存在");
        }
        return toVO(p, null);
    }

    /**
     * 跳转投保演示（返回模拟链接 + 合规声明）
     * <p>
     * 不发起真实投保，不收集用户信息
     */
    public InsuranceApplyResult applyDemo(Long userId, Long productId) {
        BizInsuranceProduct p = productMapper.selectById(productId);
        if (p == null) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "保险产品不存在");
        }
        if (!"ACTIVE".equals(p.getStatus())) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "产品已下架");
        }

        InsuranceApplyResult result = new InsuranceApplyResult();
        result.setProductId(p.getId());
        result.setProductCode(p.getProductCode());
        result.setProductName(p.getProductName());
        result.setInsurer(p.getInsurer());
        result.setApplyUrl(p.getApplyUrl());
        result.setApplyStatus("DEMO_LINK_GENERATED");
        result.setComplianceNotice(COMPLIANCE_NOTICE);
        result.setNotice("模拟演示：未发起真实投保，未向保险机构传输任何用户信息");

        log.info("[保险代销-跳转演示] 用户={}, 产品={}, 生成模拟链接", userId, p.getProductCode());
        return result;
    }

    // ============================================================
    //  工具方法
    // ============================================================

    private InsuranceProductVO toVO(BizInsuranceProduct p, String userType) {
        InsuranceProductVO vo = new InsuranceProductVO();
        vo.setId(p.getId());
        vo.setProductCode(p.getProductCode());
        vo.setProductName(p.getProductName());
        vo.setInsuranceType(p.getInsuranceType());
        vo.setInsuranceTypeName(insuranceTypeName(p.getInsuranceType()));
        vo.setScene(p.getScene());
        vo.setSceneName(sceneName(p.getScene()));
        vo.setPremiumRate(p.getPremiumRate());
        vo.setCoverageAmount(p.getCoverageAmount());
        vo.setInsurer(p.getInsurer());
        vo.setProductElements(p.getProductElements());
        vo.setConditions(p.getConditions());
        vo.setApplyUrl(p.getApplyUrl());
        vo.setSortOrder(p.getSortOrder());
        vo.setComplianceNotice(COMPLIANCE_NOTICE);

        List<String> crowdList = p.getTargetCrowd() != null
                ? Arrays.asList(p.getTargetCrowd().split(","))
                : List.of();
        vo.setTargetCrowdList(crowdList);

        if (userType != null) {
            boolean matched = crowdList.contains(userType);
            vo.setMatched(matched);
            vo.setMatchReason(matched
                    ? "您的人群类型「" + crowdTypeName(userType) + "」符合该产品投保人群范围"
                    : "该产品适用人群：" + crowdList.stream().map(this::crowdTypeName).collect(Collectors.joining("、")));
        } else {
            vo.setMatched(false);
        }
        return vo;
    }

    private String insuranceTypeName(String type) {
        return switch (type) {
            case "PERFORMANCE_BOND" -> "履约保证保险";
            case "IP_PATENT" -> "专利执行保险";
            case "IP_INFRINGEMENT" -> "侵权责任保险";
            case "PROPERTY" -> "财产综合险";
            default -> type;
        };
    }

    private String sceneName(String scene) {
        return switch (scene) {
            case "CONTRACT" -> "合同履约场景";
            case "IP" -> "知识产权场景";
            case "PROPERTY" -> "财产保障场景";
            case "EMPLOYER" -> "雇主责任场景";
            default -> scene;
        };
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

    /**
     * 跳转投保演示结果
     */
    @lombok.Data
    public static class InsuranceApplyResult {
        private Long productId;
        private String productCode;
        private String productName;
        private String insurer;
        private String applyUrl;
        private String applyStatus;
        private String complianceNotice;
        private String notice;
    }
}
