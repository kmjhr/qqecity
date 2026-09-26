package com.icbc.qingqi.module.guarantee.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.icbc.qingqi.common.BizException;
import com.icbc.qingqi.common.ErrorCode;
import com.icbc.qingqi.module.guarantee.dto.GuaranteeApplyDTO;
import com.icbc.qingqi.module.guarantee.dto.GuaranteeApplicationVO;
import com.icbc.qingqi.module.guarantee.dto.GuaranteeVO;
import com.icbc.qingqi.module.guarantee.entity.*;
import com.icbc.qingqi.module.guarantee.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 安居保函服务
 * <p>
 * 主链路：申请(SUBMITTED) → 房东确认(LANDLORD_CONFIRM) → AI复审(AI_REVIEW/MANUAL_REVIEW) → 待缴费(PENDING_PAY) → 缴费开函(APPROVED)
 * <p>
 * 银行能力均由服务层规则模拟：
 * - AI 合同复审：关键词规则（租金贷/霸王条款/押金异常/租期矛盾），命中风险词转人工复核
 * - 保函费率：演示值 0.8%—1.5%
 */
@Service
public class GuaranteeService {

    private final BizGuaranteeApplicationMapper applicationMapper;
    private final BizGuaranteeMapper guaranteeMapper;
    private final BizHouseMapper houseMapper;
    private final BizRentalContractMapper contractMapper;
    private final BizLandlordMapper landlordMapper;

    public GuaranteeService(BizGuaranteeApplicationMapper applicationMapper,
                            BizGuaranteeMapper guaranteeMapper,
                            BizHouseMapper houseMapper,
                            BizRentalContractMapper contractMapper,
                            BizLandlordMapper landlordMapper) {
        this.applicationMapper = applicationMapper;
        this.guaranteeMapper = guaranteeMapper;
        this.houseMapper = houseMapper;
        this.contractMapper = contractMapper;
        this.landlordMapper = landlordMapper;
    }

    // ============================================================
    //  保函申请
    // ============================================================

    /**
     * 租客提交保函申请
     */
    @Transactional
    public GuaranteeApplicationVO apply(Long tenantId, GuaranteeApplyDTO dto) {
        // 校验房屋存在
        BizHouse house = houseMapper.selectById(dto.getHouseId());
        if (house == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "房屋不存在");
        }
        // 校验合同存在
        BizRentalContract contract = contractMapper.selectById(dto.getContractId());
        if (contract == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "租赁合同不存在");
        }

        // 计算保函费率与保费（演示费率 0.8%—1.5%，按押金金额分档）
        BigDecimal rate = calcGuaranteeRate(dto.getDepositAmount());
        BigDecimal fee = dto.getDepositAmount().multiply(rate).setScale(2, RoundingMode.HALF_UP);
        int period = dto.getGuaranteePeriodMonths() != null ? dto.getGuaranteePeriodMonths() : 12;

        BizGuaranteeApplication app = new BizGuaranteeApplication();
        app.setApplyNo(generateApplyNo());
        app.setTenantId(tenantId);
        app.setHouseId(dto.getHouseId());
        app.setContractId(dto.getContractId());
        app.setLandlordId(house.getLandlordId());
        app.setDepositAmount(dto.getDepositAmount());
        app.setGuaranteeRate(rate);
        app.setGuaranteeFee(fee);
        app.setGuaranteePeriodMonths(period);
        app.setApplicantName(dto.getApplicantName());
        app.setApplicantPhone(dto.getApplicantPhone());
        app.setLandlordName(dto.getLandlordName());
        app.setLandlordPhone(dto.getLandlordPhone());
        app.setApplyStatus("SUBMITTED");
        app.setSubmitTime(LocalDateTime.now());
        applicationMapper.insert(app);

        return toApplicationVO(app);
    }

    /**
     * 房东确认保函申请
     */
    @Transactional
    public GuaranteeApplicationVO landlordConfirm(Long landlordId, Long applicationId) {
        BizGuaranteeApplication app = getApplication(applicationId);
        if (!app.getLandlordId().equals(landlordId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作该保函申请");
        }
        if (!"SUBMITTED".equals(app.getApplyStatus())) {
            throw new BizException(ErrorCode.BIZ_ERROR, "当前状态不允许房东确认");
        }
        app.setApplyStatus("LANDLORD_CONFIRM");
        app.setLandlordConfirmTime(LocalDateTime.now());
        applicationMapper.updateById(app);

        // 房东确认后自动进入 AI 复审
        return aiReview(applicationId);
    }

    // ============================================================
    //  AI 合同复审
    // ============================================================

    /**
     * AI 合同复审（规则模拟）
     * <p>
     * 识别维度：租金贷、霸王条款、押金异常、租期矛盾
     * 命中风险词 → 转人工复核(MANUAL_REVIEW)；否则自动通过(PASS)
     */
    @Transactional
    public GuaranteeApplicationVO aiReview(Long applicationId) {
        BizGuaranteeApplication app = getApplication(applicationId);
        if (!"LANDLORD_CONFIRM".equals(app.getApplyStatus()) && !"MANUAL_REVIEW".equals(app.getApplyStatus())) {
            throw new BizException(ErrorCode.BIZ_ERROR, "当前状态不允许 AI 复审");
        }

        // 模拟合同文本（实际应由合同上传解析，演示用申请信息拼接）
        String contractText = buildContractText(app);

        AiReviewResult result = runAiReview(contractText, app);
        app.setAiReviewResult(result.result);
        app.setAiReviewScore(result.score);
        app.setAiReviewDetail(result.detail);

        if ("PASS".equals(result.result)) {
            app.setApplyStatus("PENDING_PAY");
        } else {
            // 命中风险词，转人工复核（演示中人工复核自动通过，见 manualReviewPass）
            app.setApplyStatus("MANUAL_REVIEW");
        }
        app.setReviewTime(LocalDateTime.now());
        applicationMapper.updateById(app);
        return toApplicationVO(app);
    }

    /**
     * 人工复核通过（演示：人工复核自动通过并标注）
     */
    @Transactional
    public GuaranteeApplicationVO manualReviewPass(Long applicationId) {
        BizGuaranteeApplication app = getApplication(applicationId);
        if (!"MANUAL_REVIEW".equals(app.getApplyStatus())) {
            throw new BizException(ErrorCode.BIZ_ERROR, "当前状态不允许人工复核");
        }
        app.setApplyStatus("PENDING_PAY");
        app.setAiReviewResult("PASS");
        app.setReviewTime(LocalDateTime.now());
        applicationMapper.updateById(app);
        return toApplicationVO(app);
    }

    // ============================================================
    //  缴费与开函
    // ============================================================

    /**
     * 租客缴纳保函费并开立电子保函
     */
    @Transactional
    public GuaranteeVO payAndIssue(Long tenantId, Long applicationId) {
        BizGuaranteeApplication app = getApplication(applicationId);
        if (!app.getTenantId().equals(tenantId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作该保函申请");
        }
        if (!"PENDING_PAY".equals(app.getApplyStatus())) {
            throw new BizException(ErrorCode.BIZ_ERROR, "当前状态不允许缴费");
        }

        // 开立保函
        BizGuarantee guarantee = new BizGuarantee();
        guarantee.setGuaranteeNo(generateGuaranteeNo());
        guarantee.setApplicationId(applicationId);
        guarantee.setTenantId(app.getTenantId());
        guarantee.setLandlordId(app.getLandlordId());
        guarantee.setHouseId(app.getHouseId());
        guarantee.setGuaranteeAmount(app.getDepositAmount());
        guarantee.setGuaranteeFee(app.getGuaranteeFee());
        guarantee.setEffectiveDate(LocalDate.now());
        guarantee.setExpireDate(LocalDate.now().plusMonths(app.getGuaranteePeriodMonths() != null ? app.getGuaranteePeriodMonths() : 12));
        guarantee.setGuaranteeStatus("ACTIVE");
        guarantee.setPayStatus("PAID");
        guarantee.setPayTime(LocalDateTime.now());
        guarantee.setIssueTime(LocalDateTime.now());
        guaranteeMapper.insert(guarantee);

        // 更新申请状态
        app.setApplyStatus("APPROVED");
        applicationMapper.updateById(app);

        return toGuaranteeVO(guarantee);
    }

    // ============================================================
    //  查询
    // ============================================================

    /**
     * 分页查询当前用户的保函申请
     */
    public Page<GuaranteeApplicationVO> pageApplications(Long tenantId, int pageNum, int pageSize, String status) {
        Page<BizGuaranteeApplication> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BizGuaranteeApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizGuaranteeApplication::getTenantId, tenantId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(BizGuaranteeApplication::getApplyStatus, status);
        }
        wrapper.orderByDesc(BizGuaranteeApplication::getCreateTime);
        Page<BizGuaranteeApplication> result = applicationMapper.selectPage(page, wrapper);

        Page<GuaranteeApplicationVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toApplicationVO).toList());
        return voPage;
    }

    /**
     * 获取保函申请详情
     */
    public GuaranteeApplicationVO getApplicationDetail(Long tenantId, Long applicationId) {
        BizGuaranteeApplication app = getApplication(applicationId);
        if (!app.getTenantId().equals(tenantId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权查看该保函申请");
        }
        return toApplicationVO(app);
    }

    /**
     * 分页查询当前用户的保函
     */
    public Page<GuaranteeVO> pageGuarantees(Long tenantId, int pageNum, int pageSize, String status) {
        Page<BizGuarantee> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BizGuarantee> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizGuarantee::getTenantId, tenantId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(BizGuarantee::getGuaranteeStatus, status);
        }
        wrapper.orderByDesc(BizGuarantee::getCreateTime);
        Page<BizGuarantee> result = guaranteeMapper.selectPage(page, wrapper);

        Page<GuaranteeVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toGuaranteeVO).toList());
        return voPage;
    }

    /**
     * 获取保函详情
     */
    public GuaranteeVO getGuaranteeDetail(Long tenantId, Long guaranteeId) {
        BizGuarantee guarantee = guaranteeMapper.selectById(guaranteeId);
        if (guarantee == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "保函不存在");
        }
        if (!guarantee.getTenantId().equals(tenantId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权查看该保函");
        }
        return toGuaranteeVO(guarantee);
    }

    /**
     * 获取可选房屋列表（演示：返回全部已上架房屋）
     */
    public List<BizHouse> listHouses() {
        return houseMapper.selectList(new LambdaQueryWrapper<BizHouse>().eq(BizHouse::getStatus, 1));
    }

    // ============================================================
    //  内部方法
    // ============================================================

    private BizGuaranteeApplication getApplication(Long id) {
        BizGuaranteeApplication app = applicationMapper.selectById(id);
        if (app == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "保函申请不存在");
        }
        return app;
    }

    /**
     * 计算保函费率（演示 0.8%—1.5%）
     */
    private BigDecimal calcGuaranteeRate(BigDecimal depositAmount) {
        if (depositAmount.compareTo(new BigDecimal("3000")) <= 0) {
            return new BigDecimal("0.0150"); // 1.5%
        } else if (depositAmount.compareTo(new BigDecimal("5000")) <= 0) {
            return new BigDecimal("0.0120"); // 1.2%
        } else {
            return new BigDecimal("0.0080"); // 0.8%
        }
    }

    /**
     * AI 复审规则引擎
     */
    private AiReviewResult runAiReview(String text, BizGuaranteeApplication app) {
        int score = 100;
        StringBuilder detail = new StringBuilder();
        boolean risk = false;

        // 1. 租金贷风险词
        if (text.contains("租金贷") || text.contains("分期付租") || text.contains("贷款付租")) {
            score -= 40;
            detail.append("命中风险词[租金贷];");
            risk = true;
        }
        // 2. 霸王条款
        if (text.contains("概不退还") || text.contains("不退押金") || text.contains("一切责任")) {
            score -= 30;
            detail.append("命中风险词[霸王条款];");
            risk = true;
        }
        // 3. 押金异常（超过 2 个月租金视为异常）
        // 演示：无合同明细时跳过，依赖申请金额与房屋月租金对比
        BizHouse house = houseMapper.selectById(app.getHouseId());
        if (house != null && app.getDepositAmount() != null) {
            BigDecimal twoMonthsRent = house.getMonthlyRent().multiply(new BigDecimal("2"));
            if (app.getDepositAmount().compareTo(twoMonthsRent) > 0) {
                score -= 25;
                detail.append("押金异常(超过2个月租金);");
                risk = true;
            }
        }
        // 4. 租期矛盾（结束日不晚于开始日）
        if (app.getGuaranteePeriodMonths() != null && app.getGuaranteePeriodMonths() <= 0) {
            score -= 20;
            detail.append("租期矛盾(期限<=0);");
            risk = true;
        }

        if (score < 0) score = 0;
        if (detail.length() == 0) {
            detail.append("未命中风险词，合同要素合规");
        }
        return new AiReviewResult(risk ? "RISK_WARNING" : "PASS", score, detail.toString());
    }

    private String buildContractText(BizGuaranteeApplication app) {
        // 演示：用房东姓名、申请人姓名等拼接模拟合同文本
        return "租赁合同：房东" + (app.getLandlordName() == null ? "" : app.getLandlordName())
                + "将房屋出租给" + (app.getApplicantName() == null ? "" : app.getApplicantName());
    }

    private String generateApplyNo() {
        return "GBA" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    private String generateGuaranteeNo() {
        return "GB" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    private GuaranteeApplicationVO toApplicationVO(BizGuaranteeApplication app) {
        GuaranteeApplicationVO vo = new GuaranteeApplicationVO();
        BeanUtil.copyProperties(app, vo);
        return vo;
    }

    private GuaranteeVO toGuaranteeVO(BizGuarantee g) {
        GuaranteeVO vo = new GuaranteeVO();
        BeanUtil.copyProperties(g, vo);
        return vo;
    }

    private record AiReviewResult(String result, int score, String detail) {}
}
