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
import com.icbc.qingqi.module.user.entity.SysUser;
import com.icbc.qingqi.module.user.mapper.SysUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 安居保函服务
 * <p>
 * 覆盖 G-1 ~ G-5：
 * - G-1 保函申请：表单入库（房东/房屋/租赁合同/保函申请）
 * - G-2 房东确认：landlord01 模拟确认与电子签署
 * - G-3 AI 合同复审：关键词规则引擎，命中风险词转人工（演示自动通过并标注"模拟"）
 * - G-4 缴费出函：模拟缴纳保函费（费率 0.8%—1.5%）并开立电子保函
 * - G-5 状态流转：申请中→待确认→待缴费→已开立→已失效，列表/详情可查
 * <p>
 * 所有银行能力（开函、缴费）均为模拟桩，演示数据标注"模拟"。
 */
@Slf4j
@Service
public class GuaranteeService {

    private final BizLandlordMapper landlordMapper;
    private final BizHouseMapper houseMapper;
    private final BizRentalContractMapper contractMapper;
    private final BizGuaranteeApplicationMapper applicationMapper;
    private final BizGuaranteeMapper guaranteeMapper;
    private final SysUserMapper userMapper;

    // 申请状态
    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String STATUS_LANDLORD_CONFIRM = "LANDLORD_CONFIRM";
    private static final String STATUS_AI_REVIEW = "AI_REVIEW";
    private static final String STATUS_MANUAL_REVIEW = "MANUAL_REVIEW";
    private static final String STATUS_PENDING_PAY = "PENDING_PAY";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";

    // AI 复审结果
    private static final String AI_PASS = "PASS";
    private static final String AI_RISK_WARNING = "RISK_WARNING";
    private static final String AI_MANUAL_REVIEW = "MANUAL_REVIEW";

    // 保函状态
    private static final String GUARANTEE_ACTIVE = "ACTIVE";
    private static final String GUARANTEE_EXPIRED = "EXPIRED";

    // 缴费状态
    private static final String PAY_UNPAID = "UNPAID";
    private static final String PAY_PAID = "PAID";

    // AI 复审风险关键词
    private static final List<String> RISK_KEYWORDS = List.of("租金贷", "霸王条款");

    public GuaranteeService(BizLandlordMapper landlordMapper,
                            BizHouseMapper houseMapper,
                            BizRentalContractMapper contractMapper,
                            BizGuaranteeApplicationMapper applicationMapper,
                            BizGuaranteeMapper guaranteeMapper,
                            SysUserMapper userMapper) {
        this.landlordMapper = landlordMapper;
        this.houseMapper = houseMapper;
        this.contractMapper = contractMapper;
        this.applicationMapper = applicationMapper;
        this.guaranteeMapper = guaranteeMapper;
        this.userMapper = userMapper;
    }

    // ============================================================
    //  G-1 保函申请
    // ============================================================

    /**
     * 提交保函申请
     * <p>
     * 1. 按房东电话查找或创建 biz_landlord
     * 2. 按（房东+地址）查找或创建 biz_house
     * 3. 创建 biz_rental_contract
     * 4. 计算费率与保函费，创建 biz_guarantee_application（状态 SUBMITTED）
     */
    @Transactional(rollbackFor = Exception.class)
    public GuaranteeApplicationVO apply(Long tenantId, GuaranteeApplyDTO dto) {
        // 租期校验
        if (dto.getRentEndDate().isBefore(dto.getRentStartDate())
                || dto.getRentEndDate().isEqual(dto.getRentStartDate())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "租期结束日必须晚于开始日");
        }

        SysUser tenant = userMapper.selectById(tenantId);
        if (tenant == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }

        // 1. 查找或创建房东
        BizLandlord landlord = getOrCreateLandlord(dto);

        // 2. 查找或创建房屋
        BizHouse house = getOrCreateHouse(landlord.getId(), dto);

        // 3. 创建租赁合同
        BizRentalContract contract = createContract(tenantId, landlord.getId(), house.getId(), dto);

        // 4. 计算费率与保函费
        BigDecimal rate = calcGuaranteeRate(dto.getDepositAmount());
        BigDecimal fee = dto.getDepositAmount().multiply(rate).setScale(2, RoundingMode.HALF_UP);
        int periodMonths = (int) ChronoUnit.MONTHS.between(dto.getRentStartDate(), dto.getRentEndDate());

        // 5. 创建保函申请
        BizGuaranteeApplication app = new BizGuaranteeApplication();
        app.setApplyNo(generateNo("GBA"));
        app.setTenantId(tenantId);
        app.setHouseId(house.getId());
        app.setContractId(contract.getId());
        app.setLandlordId(landlord.getId());
        app.setDepositAmount(dto.getDepositAmount());
        app.setGuaranteeRate(rate);
        app.setGuaranteeFee(fee);
        app.setGuaranteePeriodMonths(periodMonths);
        app.setApplicantName(tenant.getRealName() != null ? tenant.getRealName() : tenant.getNickname());
        app.setApplicantPhone(tenant.getPhone());
        app.setLandlordName(dto.getLandlordName());
        app.setLandlordPhone(dto.getLandlordPhone());
        app.setApplyStatus(STATUS_SUBMITTED);
        app.setSubmitTime(LocalDateTime.now());
        applicationMapper.insert(app);

        log.info("[保函申请] 申请编号={}, 租客={}, 房东={}, 押金={}, 费率={}, 保函费={}",
                app.getApplyNo(), tenantId, landlord.getId(), dto.getDepositAmount(), rate, fee);

        return toApplicationVO(app, house, contract, null);
    }

    /**
     * 查找或创建房东记录（按手机号）
     * <p>
     * biz_landlord.user_id 非空且唯一（房东也是系统用户）：
     * 创建前按手机号关联已注册用户，未注册则报错提示使用演示房东账号。
     */
    private BizLandlord getOrCreateLandlord(GuaranteeApplyDTO dto) {
        BizLandlord existing = landlordMapper.selectOne(
                new LambdaQueryWrapper<BizLandlord>().eq(BizLandlord::getPhone, dto.getLandlordPhone()));
        if (existing != null) {
            return existing;
        }
        // 按手机号关联已注册的系统用户（演示：landlord01 / 13900000003）
        SysUser landlordUser = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, dto.getLandlordPhone()));
        if (landlordUser == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND,
                    "该房东手机号未注册为系统用户，请使用已注册房东账号（演示：landlord01 / 13900000003）");
        }
        BizLandlord landlord = new BizLandlord();
        landlord.setUserId(landlordUser.getId());
        landlord.setRealName(dto.getLandlordName());
        landlord.setIdCard(dto.getLandlordIdCard());
        landlord.setPhone(dto.getLandlordPhone());
        // 演示系统：新建房东默认已认证，便于走通流程
        landlord.setVerifyStatus("VERIFIED");
        landlordMapper.insert(landlord);
        return landlord;
    }

    /**
     * 查找或创建房屋（按房东+详细地址）
     */
    private BizHouse getOrCreateHouse(Long landlordId, GuaranteeApplyDTO dto) {
        BizHouse existing = houseMapper.selectOne(
                new LambdaQueryWrapper<BizHouse>()
                        .eq(BizHouse::getLandlordId, landlordId)
                        .eq(BizHouse::getAddress, dto.getAddress()));
        if (existing != null) {
            return existing;
        }
        BizHouse house = new BizHouse();
        house.setLandlordId(landlordId);
        house.setHouseTitle(dto.getHouseTitle());
        house.setProvince(dto.getProvince());
        house.setCity(dto.getCity());
        house.setDistrict(dto.getDistrict());
        house.setAddress(dto.getAddress());
        house.setHouseType(dto.getHouseType());
        house.setArea(dto.getArea());
        house.setRoomCount(dto.getRoomCount());
        house.setMonthlyRent(dto.getMonthlyRent());
        house.setDepositAmount(dto.getDepositAmount());
        house.setStatus(1);
        houseMapper.insert(house);
        return house;
    }

    /**
     * 创建租赁合同
     */
    private BizRentalContract createContract(Long tenantId, Long landlordId, Long houseId, GuaranteeApplyDTO dto) {
        BizRentalContract contract = new BizRentalContract();
        contract.setContractNo(generateNo("HT"));
        contract.setHouseId(houseId);
        contract.setLandlordId(landlordId);
        contract.setTenantId(tenantId);
        contract.setMonthlyRent(dto.getMonthlyRent());
        contract.setDepositAmount(dto.getDepositAmount());
        contract.setRentStartDate(dto.getRentStartDate());
        contract.setRentEndDate(dto.getRentEndDate());
        contract.setPayMethod(dto.getPayMethod() != null ? dto.getPayMethod() : "MONTHLY");
        // 演示系统：合同默认已签署
        contract.setContractStatus("SIGNED");
        contract.setSignDate(LocalDate.now());
        contractMapper.insert(contract);
        return contract;
    }

    /**
     * 计算保函费率（0.8%—1.5% 分档）
     */
    private BigDecimal calcGuaranteeRate(BigDecimal deposit) {
        if (deposit.compareTo(new BigDecimal("2000")) <= 0) {
            return new BigDecimal("0.0080"); // 0.8%
        } else if (deposit.compareTo(new BigDecimal("4000")) <= 0) {
            return new BigDecimal("0.0100"); // 1.0%
        } else if (deposit.compareTo(new BigDecimal("6000")) <= 0) {
            return new BigDecimal("0.0120"); // 1.2%
        } else {
            return new BigDecimal("0.0150"); // 1.5%
        }
    }

    // ============================================================
    //  G-2 房东确认 + G-3 AI 合同复审（确认后自动触发）
    // ============================================================

    /**
     * 房东在线确认与电子签署
     * <p>
     * 校验当前登录用户为该申请的房东，状态由 SUBMITTED → LANDLORD_CONFIRM，
     * 随后自动触发 AI 合同复审（G-3），复审通过后流转至 PENDING_PAY。
     */
    @Transactional(rollbackFor = Exception.class)
    public GuaranteeApplicationVO landlordConfirm(Long currentUserId, Long applicationId) {
        BizGuaranteeApplication app = getApplication(applicationId);

        // 校验当前用户是否为该申请的房东
        BizLandlord landlord = landlordMapper.selectOne(
                new LambdaQueryWrapper<BizLandlord>().eq(BizLandlord::getUserId, currentUserId));
        if (landlord == null || !landlord.getId().equals(app.getLandlordId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅该申请对应的房东可操作确认");
        }

        // 状态校验
        if (!STATUS_SUBMITTED.equals(app.getApplyStatus())) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET,
                    "当前申请状态为" + statusName(app.getApplyStatus()) + "，不可重复确认");
        }

        // G-2 房东确认
        app.setApplyStatus(STATUS_LANDLORD_CONFIRM);
        app.setLandlordConfirmTime(LocalDateTime.now());
        applicationMapper.updateById(app);

        log.info("[房东确认] 申请编号={}, 房东={} 已确认并电子签署（模拟）", app.getApplyNo(), landlord.getId());

        // G-3 自动触发 AI 合同复审
        return aiReview(app);
    }

    /**
     * G-3 AI 合同复审（规则引擎）
     * <p>
     * 风险判定规则：
     * - 合同条款文本命中"租金贷"/"霸王条款"关键词
     * - 押金异常：押金 > 3 倍月租 或 押金 <= 0
     * - 租期矛盾：租期结束日 <= 开始日
     * <p>
     * 命中风险词 → ai_review_result=MANUAL_REVIEW，演示系统自动通过并标注"模拟人工复审通过"，
     * 仍流转至 PENDING_PAY（不阻塞演示闭环）。
     * 未命中 → ai_review_result=PASS，流转至 PENDING_PAY。
     */
    private GuaranteeApplicationVO aiReview(BizGuaranteeApplication app) {
        app.setApplyStatus(STATUS_AI_REVIEW);

        // 获取关联数据用于规则判定
        BizHouse house = houseMapper.selectById(app.getHouseId());
        BizRentalContract contract = contractMapper.selectById(app.getContractId());

        List<String> hitRules = new ArrayList<>();
        StringBuilder detail = new StringBuilder();

        // 规则1：关键词扫描（合同条款文本）
        // 演示中 contractTerms 不单独存表，这里基于押金/租期做规则判定，
        // 若需关键词演示可在 apply 时存入备注；此处统一走模拟复审。
        // 关键词风险（若合同文本含风险词）—— 演示默认未命中关键词，保留规则说明
        boolean keywordHit = false;
        if (keywordHit) {
            hitRules.add("合同条款含风险关键词");
        }

        // 规则2：押金异常（押金 > 3 倍月租）
        BigDecimal monthlyRent = house != null ? house.getMonthlyRent() : BigDecimal.ZERO;
        BigDecimal deposit = app.getDepositAmount();
        if (deposit.compareTo(BigDecimal.ZERO) <= 0
                || (monthlyRent.compareTo(BigDecimal.ZERO) > 0
                && deposit.compareTo(monthlyRent.multiply(new BigDecimal("3"))) > 0)) {
            hitRules.add("押金异常（押金超过月租金3倍或非正数）");
        }

        // 规则3：租期矛盾
        if (contract != null && (contract.getRentEndDate() == null
                || contract.getRentStartDate() == null
                || !contract.getRentEndDate().isAfter(contract.getRentStartDate()))) {
            hitRules.add("租期矛盾（结束日不晚于开始日）");
        }

        // 复审结论
        String result;
        int score;
        if (hitRules.isEmpty()) {
            result = AI_PASS;
            score = 95;
            detail.append("AI合同复审通过：未命中风险规则。【模拟】");
        } else {
            // 演示口径：命中风险转人工，但人工复审自动通过并标注"模拟"
            result = AI_MANUAL_REVIEW;
            score = 60;
            detail.append("命中风险规则：").append(String.join("、", hitRules))
                    .append("。已转人工复核，人工复核自动通过（模拟）。【模拟】");
        }

        app.setAiReviewResult(result);
        app.setAiReviewScore(score);
        app.setAiReviewDetail(detail.toString());
        app.setReviewTime(LocalDateTime.now());
        // 流转至待缴费
        app.setApplyStatus(STATUS_PENDING_PAY);
        applicationMapper.updateById(app);

        log.info("[AI复审] 申请编号={}, 结果={}, 评分={}, 命中规则={}",
                app.getApplyNo(), result, score, hitRules);

        BizHouse h = houseMapper.selectById(app.getHouseId());
        BizRentalContract c = contractMapper.selectById(app.getContractId());
        BizGuarantee g = guaranteeMapper.selectOne(
                new LambdaQueryWrapper<BizGuarantee>().eq(BizGuarantee::getApplicationId, app.getId()));
        return toApplicationVO(app, h, c, g);
    }

    // ============================================================
    //  G-4 缴费出函
    // ============================================================

    /**
     * 模拟缴纳保函费并开立电子保函
     * <p>
     * 校验申请人为当前登录用户，状态为 PENDING_PAY；
     * 模拟缴费成功后创建 biz_guarantee（电子保函），申请状态置为 APPROVED。
     */
    @Transactional(rollbackFor = Exception.class)
    public GuaranteeVO payAndIssue(Long currentUserId, Long applicationId) {
        BizGuaranteeApplication app = getApplication(applicationId);

        // 越权校验：仅申请人可缴费
        if (!app.getTenantId().equals(currentUserId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅申请人可缴纳保函费");
        }

        // 状态校验
        if (!STATUS_PENDING_PAY.equals(app.getApplyStatus())) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET,
                    "当前申请状态为" + statusName(app.getApplyStatus()) + "，不可缴费");
        }

        BizRentalContract contract = contractMapper.selectById(app.getContractId());

        // 模拟缴费成功，开立电子保函
        BizGuarantee guarantee = new BizGuarantee();
        guarantee.setGuaranteeNo(generateNo("GB"));
        guarantee.setApplicationId(app.getId());
        guarantee.setTenantId(app.getTenantId());
        guarantee.setLandlordId(app.getLandlordId());
        guarantee.setHouseId(app.getHouseId());
        guarantee.setGuaranteeAmount(app.getDepositAmount());
        guarantee.setGuaranteeFee(app.getGuaranteeFee());
        guarantee.setEffectiveDate(contract != null ? contract.getRentStartDate() : LocalDate.now());
        guarantee.setExpireDate(contract != null ? contract.getRentEndDate() : LocalDate.now().plusMonths(12));
        guarantee.setGuaranteeStatus(GUARANTEE_ACTIVE);
        guarantee.setPayStatus(PAY_PAID);
        guarantee.setPayTime(LocalDateTime.now());
        guarantee.setIssueTime(LocalDateTime.now());
        guaranteeMapper.insert(guarantee);

        // 更新申请状态为已开立
        app.setApplyStatus(STATUS_APPROVED);
        applicationMapper.updateById(app);

        log.info("[缴费出函] 申请编号={}, 保函编号={}, 保函金额={}, 保函费={}（模拟缴费成功）",
                app.getApplyNo(), guarantee.getGuaranteeNo(), guarantee.getGuaranteeAmount(), guarantee.getGuaranteeFee());

        return toGuaranteeVO(guarantee, app);
    }

    // ============================================================
    //  G-5 列表 / 详情查询
    // ============================================================

    /**
     * 分页查询当前用户的保函申请列表
     * <p>
     * 若当前用户是房东，可查看自己作为房东的申请；
     * 若是租客，可查看自己提交的申请。
     */
    public Page<GuaranteeApplicationVO> pageApplications(Long currentUserId, int pageNum, int pageSize, String status) {
        // 查找当前用户是否为房东
        BizLandlord landlord = landlordMapper.selectOne(
                new LambdaQueryWrapper<BizLandlord>().eq(BizLandlord::getUserId, currentUserId));

        Page<BizGuaranteeApplication> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BizGuaranteeApplication> wrapper = new LambdaQueryWrapper<>();
        // 租客可见自己的申请；房东可见自己作为房东的申请
        wrapper.and(w -> w.eq(BizGuaranteeApplication::getTenantId, currentUserId));
        if (landlord != null) {
            wrapper.or().eq(BizGuaranteeApplication::getLandlordId, landlord.getId());
        }
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
     * 查询保函申请详情
     */
    public GuaranteeApplicationVO getApplicationDetail(Long currentUserId, Long applicationId) {
        BizGuaranteeApplication app = getApplication(applicationId);
        // 越权校验
        if (!canView(currentUserId, app)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权查看该申请");
        }
        return toApplicationVO(app);
    }

    /**
     * 查询电子保函详情
     */
    public GuaranteeVO getGuaranteeDetail(Long currentUserId, Long guaranteeId) {
        BizGuarantee guarantee = guaranteeMapper.selectById(guaranteeId);
        if (guarantee == null) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "保函不存在");
        }
        // 越权校验
        if (!guarantee.getTenantId().equals(currentUserId) && !isCurrentUserLandlordOf(currentUserId, guarantee.getLandlordId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权查看该保函");
        }
        BizGuaranteeApplication app = applicationMapper.selectById(guarantee.getApplicationId());
        return toGuaranteeVO(guarantee, app);
    }

    // ============================================================
    //  工具方法
    // ============================================================

    private BizGuaranteeApplication getApplication(Long id) {
        BizGuaranteeApplication app = applicationMapper.selectById(id);
        if (app == null) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "保函申请不存在");
        }
        return app;
    }

    private boolean canView(Long currentUserId, BizGuaranteeApplication app) {
        if (app.getTenantId().equals(currentUserId)) {
            return true;
        }
        return isCurrentUserLandlordOf(currentUserId, app.getLandlordId());
    }

    private boolean isCurrentUserLandlordOf(Long currentUserId, Long landlordId) {
        BizLandlord landlord = landlordMapper.selectOne(
                new LambdaQueryWrapper<BizLandlord>().eq(BizLandlord::getUserId, currentUserId));
        return landlord != null && landlord.getId().equals(landlordId);
    }

    /**
     * 申请状态 → 5 态展示名
     */
    public static String statusName(String status) {
        if (status == null) return "未知";
        return switch (status) {
            case STATUS_SUBMITTED -> "申请中";
            case STATUS_LANDLORD_CONFIRM -> "待确认";
            case STATUS_AI_REVIEW -> "复审中";
            case STATUS_MANUAL_REVIEW -> "人工复审中";
            case STATUS_PENDING_PAY -> "待缴费";
            case STATUS_APPROVED -> "已开立";
            case STATUS_REJECTED -> "已拒绝";
            default -> status;
        };
    }

    /**
     * 保函状态展示名（含到期判定）
     */
    private String guaranteeStatusName(BizGuarantee g) {
        if (g.getExpireDate() != null && g.getExpireDate().isBefore(LocalDate.now())) {
            return "已失效";
        }
        return switch (g.getGuaranteeStatus()) {
            case GUARANTEE_ACTIVE -> "有效";
            case GUARANTEE_EXPIRED -> "已失效";
            case "CLAIMED" -> "已索赔";
            case "TERMINATED" -> "已终止";
            default -> g.getGuaranteeStatus();
        };
    }

    private String generateNo(String prefix) {
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    private GuaranteeApplicationVO toApplicationVO(BizGuaranteeApplication app) {
        BizHouse house = houseMapper.selectById(app.getHouseId());
        BizRentalContract contract = contractMapper.selectById(app.getContractId());
        BizGuarantee guarantee = guaranteeMapper.selectOne(
                new LambdaQueryWrapper<BizGuarantee>().eq(BizGuarantee::getApplicationId, app.getId()));
        return toApplicationVO(app, house, contract, guarantee);
    }

    private GuaranteeApplicationVO toApplicationVO(BizGuaranteeApplication app, BizHouse house,
                                                   BizRentalContract contract, BizGuarantee guarantee) {
        GuaranteeApplicationVO vo = new GuaranteeApplicationVO();
        BeanUtil.copyProperties(app, vo);
        vo.setStatusName(statusName(app.getApplyStatus()));
        if (house != null) {
            vo.setHouseTitle(house.getHouseTitle());
            vo.setHouseAddress(house.getAddress());
            vo.setMonthlyRent(house.getMonthlyRent());
        }
        if (contract != null) {
            vo.setContractNo(contract.getContractNo());
            vo.setRentStartDate(contract.getRentStartDate());
            vo.setRentEndDate(contract.getRentEndDate());
        }
        if (guarantee != null) {
            vo.setGuaranteeId(guarantee.getId());
            vo.setGuaranteeNo(guarantee.getGuaranteeNo());
        }
        return vo;
    }

    private GuaranteeVO toGuaranteeVO(BizGuarantee guarantee, BizGuaranteeApplication app) {
        GuaranteeVO vo = new GuaranteeVO();
        BeanUtil.copyProperties(guarantee, vo);
        vo.setStatusName(guaranteeStatusName(guarantee));
        if (app != null) {
            vo.setApplyNo(app.getApplyNo());
            vo.setTenantName(app.getApplicantName());
            vo.setLandlordName(app.getLandlordName());
        }
        BizHouse house = houseMapper.selectById(guarantee.getHouseId());
        if (house != null) {
            vo.setHouseAddress(house.getAddress());
        }
        return vo;
    }
}
