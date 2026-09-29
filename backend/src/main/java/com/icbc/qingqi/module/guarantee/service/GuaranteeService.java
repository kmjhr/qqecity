package com.icbc.qingqi.module.guarantee.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.icbc.qingqi.common.BizException;
import com.icbc.qingqi.common.ErrorCode;
import com.icbc.qingqi.module.admin.service.DemoAutoApproveService;
import com.icbc.qingqi.module.guarantee.dto.GuaranteeApplyDTO;
import com.icbc.qingqi.module.guarantee.dto.MoveoutRecordDTO;
import com.icbc.qingqi.module.guarantee.dto.GuaranteeApplicationVO;
import com.icbc.qingqi.module.guarantee.dto.GuaranteeVO;
import com.icbc.qingqi.module.guarantee.dto.LandlordSignDTO;
import com.icbc.qingqi.module.guarantee.entity.*;
import com.icbc.qingqi.module.guarantee.mapper.*;
import com.icbc.qingqi.module.message.entity.SysMessage;
import com.icbc.qingqi.module.message.mapper.SysMessageMapper;
import com.icbc.qingqi.module.pay.dto.PayOrderVO;
import com.icbc.qingqi.module.pay.service.PayService;
import com.icbc.qingqi.module.pay.service.PaySuccessEvent;
import com.icbc.qingqi.module.user.entity.SysUser;
import com.icbc.qingqi.security.UserContext;
import com.icbc.qingqi.module.user.mapper.SysUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
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
    private final BizMoveoutRecordMapper moveoutRecordMapper;
    private final BizGuaranteeApplicationMapper applicationMapper;
    private final BizGuaranteeMapper guaranteeMapper;
    private final BizGuaranteeClaimMapper claimMapper;
    private final SysUserMapper userMapper;
    private final SysMessageMapper messageMapper;
    private final PayService payService;
    private final DemoAutoApproveService demoAutoApprove;

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

    // 房屋租住情况（租期前/中/后 × 索赔 × 留档确认）
    private static final String SIT_PRE_RENTAL = "PRE_RENTAL";
    private static final String SIT_RENTING_CLAIMED = "RENTING_CLAIMED";
    private static final String SIT_ENDED_CLAIMED = "ENDED_CLAIMED";
    private static final String SIT_RENTING_NORMAL = "RENTING_NORMAL";
    private static final String SIT_ENDED_NORMAL = "ENDED_NORMAL";
    private static final String SIT_ENDED_CONFIRMED = "ENDED_CONFIRMED";
    private static final String SIT_ENDED_PENDING_CONFIRM = "ENDED_PENDING_CONFIRM";
    private static final String SIT_ENDED_EXPIRED = "ENDED_EXPIRED";

    // 缴费状态
    private static final String PAY_UNPAID = "UNPAID";
    private static final String PAY_PAID = "PAID";

    // AI 复审风险关键词
    private static final List<String> RISK_KEYWORDS = List.of("租金贷", "霸王条款");

    public GuaranteeService(BizLandlordMapper landlordMapper,
                            BizHouseMapper houseMapper,
                            BizRentalContractMapper contractMapper,
                            BizMoveoutRecordMapper moveoutRecordMapper,
                            BizGuaranteeApplicationMapper applicationMapper,
                            BizGuaranteeMapper guaranteeMapper,
                            BizGuaranteeClaimMapper claimMapper,
                            SysUserMapper userMapper,
                            SysMessageMapper messageMapper,
                            PayService payService,
                            DemoAutoApproveService demoAutoApprove) {
        this.landlordMapper = landlordMapper;
        this.houseMapper = houseMapper;
        this.contractMapper = contractMapper;
        this.moveoutRecordMapper = moveoutRecordMapper;
        this.applicationMapper = applicationMapper;
        this.guaranteeMapper = guaranteeMapper;
        this.claimMapper = claimMapper;
        this.userMapper = userMapper;
        this.messageMapper = messageMapper;
        this.payService = payService;
        this.demoAutoApprove = demoAutoApprove;
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

        // 站内信：申请已提交
        sendInternalMessage(tenantId, "保函申请已提交",
                "您的保函申请（编号" + app.getApplyNo() + "）已提交成功，等待房东确认。保函费¥" + fee + "（费率" + rate + "）。【模拟】",
                "GUARANTEE", app.getId());

        // 演示模式：自动代房东确认 + AI 复审（转人工也自动通过）→ 直达待缴费，无需人工点击
        if (demoAutoApprove.isEnabled()) {
            return autoApproveFlow(app);
        }

        return toApplicationVO(app, house, contract, null);
    }

    /**
     * 演示模式自动推进：模拟房东确认 → AI 复审 → 人工复审自动通过 → 待缴费
     */
    private GuaranteeApplicationVO autoApproveFlow(BizGuaranteeApplication app) {
        // 1. 模拟房东确认 + 电子签署（演示自动）
        app.setApplyStatus(STATUS_LANDLORD_CONFIRM);
        app.setLandlordConfirmTime(LocalDateTime.now());
        app.setSignContent("DEMO_AUTO_CONFIRM");
        app.setSignTime(LocalDateTime.now());
        applicationMapper.updateById(app);

        // 2. AI 合同复审（内部若转人工，演示模式自动通过）
        GuaranteeApplicationVO vo = aiReview(app);

        // 3. 兜底：若仍停在人工复审 → 自动裁决通过
        if (STATUS_MANUAL_REVIEW.equals(app.getApplyStatus())) {
            app.setApplyStatus(STATUS_PENDING_PAY);
            app.setAiReviewDetail((app.getAiReviewDetail() != null ? app.getAiReviewDetail() : "")
                    + " | 演示模式自动通过（模拟人工复审 APPROVED）。【模拟】");
            applicationMapper.updateById(app);
            sendInternalMessage(app.getTenantId(), "人工复审通过（演示自动）",
                    "保函申请（编号" + app.getApplyNo() + "）演示模式下人工复审自动通过，请缴纳保函费¥" + app.getGuaranteeFee() + "。【模拟】",
                    "GUARANTEE", app.getId());
        }
        return vo;
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
     * 记录电子签名内容与签署时间，随后自动触发 AI 合同复审（G-3）。
     * 复审通过后流转至 PENDING_PAY；置信度不足则停在 MANUAL_REVIEW 等待人工复核。
     */
    @Transactional(rollbackFor = Exception.class)
    public GuaranteeApplicationVO landlordConfirm(Long currentUserId, Long applicationId, LandlordSignDTO signDTO) {
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

        // G-2 房东确认 + 电子签约
        app.setApplyStatus(STATUS_LANDLORD_CONFIRM);
        app.setLandlordConfirmTime(LocalDateTime.now());
        app.setSignContent(signDTO.getSignContent());
        app.setSignTime(LocalDateTime.now());
        applicationMapper.updateById(app);

        log.info("[房东确认] 申请编号={}, 房东={} 已确认并电子签署，签名方式={}（模拟）",
                app.getApplyNo(), landlord.getId(),
                "CLICK_CONFIRM".equals(signDTO.getSignContent()) ? "点击确认" : "Canvas手写");

        // 站内信：房东已确认
        sendInternalMessage(app.getTenantId(), "房东已确认保函申请",
                "房东已确认并电子签署保函申请（编号" + app.getApplyNo() + "），系统正在进行AI复审。【模拟】",
                "GUARANTEE", app.getId());

        // G-3 自动触发 AI 合同复审
        return aiReview(app);
    }

    /**
     * 管理端代房东确认（银行/运营代操作，演示降低门槛）
     * <p>
     * 与 landlordConfirm 的区别：不校验"当前用户为该申请房东"，
     * 由 ADMIN/BANK_OPERATOR 角色（JwtAuthFilter 已校验）代房东确认并电子签署，
     * 签名方式记录为 ADMIN_AGENT_CONFIRM，随后同样自动触发 AI 合同复审（G-3）。
     */
    @Transactional(rollbackFor = Exception.class)
    public GuaranteeApplicationVO adminLandlordConfirm(Long operatorId, Long applicationId, String signContent) {
        BizGuaranteeApplication app = getApplication(applicationId);

        // 状态校验
        if (!STATUS_SUBMITTED.equals(app.getApplyStatus())) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET,
                    "当前申请状态为" + statusName(app.getApplyStatus()) + "，仅待确认状态可操作");
        }

        // 代房东确认 + 电子签署（管理端代签标识）
        app.setApplyStatus(STATUS_LANDLORD_CONFIRM);
        app.setLandlordConfirmTime(LocalDateTime.now());
        app.setSignContent(signContent != null && !signContent.isBlank() ? signContent : "ADMIN_AGENT_CONFIRM");
        app.setSignTime(LocalDateTime.now());
        applicationMapper.updateById(app);

        log.info("[保函-管理端代房东确认] 申请编号={}, 操作人={}, 签名方式=ADMIN_AGENT（模拟）",
                app.getApplyNo(), operatorId);

        // 站内信：房东已确认（管理端代操作）
        sendInternalMessage(app.getTenantId(), "房东已确认保函申请（管理端代操作）",
                "房东已确认并电子签署保函申请（编号" + app.getApplyNo() + "），系统正在进行AI复审。【模拟】",
                "GUARANTEE", app.getId());

        // G-3 自动触发 AI 合同复审
        return aiReview(app);
    }

    /**
     * G-3 AI 合同复审（规则引擎 + 置信度阈值）
     * <p>
     * 风险判定规则：
     * - 合同条款文本命中"租金贷"/"霸王条款"关键词
     * - 押金异常：押金 > 3 倍月租 或 押金 <= 0
     * - 租期矛盾：租期结束日 <= 开始日
     * <p>
     * 置信度阈值（缺口 #17）：
     * - 未命中风险规则 → 置信度 95 → PASS，流转至 PENDING_PAY
     * - 命中风险规则 → 置信度 < 60 → MANUAL_REVIEW，停在人工复审等待 banker 处理（不自动 PASS）
     * banker 可通过 /v1/guarantee/{id}/manual-review 端点审核后放行或拒绝。
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

        // 复审结论（置信度阈值 60，缺口 #17）
        String result;
        int score;
        if (hitRules.isEmpty()) {
            result = AI_PASS;
            score = 95;
            detail.append("AI合同复审通过：未命中风险规则，置信度95%。【模拟】");
        } else {
            // 命中风险规则 → 置信度 < 60 → 转 MANUAL_REVIEW，不自动 PASS
            result = AI_MANUAL_REVIEW;
            score = 45;
            detail.append("命中风险规则：").append(String.join("、", hitRules))
                    .append("。置信度").append(score).append("%<60%，已转人工复核，等待banker审核。【模拟】");
        }

        app.setAiReviewResult(result);
        app.setAiReviewScore(score);
        app.setAiReviewDetail(detail.toString());
        app.setReviewTime(LocalDateTime.now());

        if (AI_PASS.equals(result)) {
            // 置信度达标，流转至待缴费
            app.setApplyStatus(STATUS_PENDING_PAY);
            // 站内信：AI复审通过
            sendInternalMessage(app.getTenantId(), "AI复审通过",
                    "保函申请（编号" + app.getApplyNo() + "）AI复审通过，置信度" + score + "%，请缴纳保函费¥" + app.getGuaranteeFee() + "。【模拟】",
                    "GUARANTEE", app.getId());
        } else {
            if (demoAutoApprove.isEnabled()) {
                // 演示模式：AI 复审转人工后自动裁决通过 → 待缴费（无需 banker 点击）
                app.setApplyStatus(STATUS_PENDING_PAY);
                app.setAiReviewDetail(detail.toString()
                        + " | 演示模式自动通过（模拟人工复审 APPROVED）。【模拟】");
                sendInternalMessage(app.getTenantId(), "AI复审转人工已自动通过（演示）",
                        "保函申请（编号" + app.getApplyNo() + "）AI复审转人工，演示模式下已自动通过，请缴纳保函费¥" + app.getGuaranteeFee() + "。【模拟】",
                        "GUARANTEE", app.getId());
            } else {
                // 置信度不足，停在人工复审
                app.setApplyStatus(STATUS_MANUAL_REVIEW);
                // 站内信：AI复审转人工
                sendInternalMessage(app.getTenantId(), "AI复审转人工复核",
                        "保函申请（编号" + app.getApplyNo() + "）AI复审置信度" + score + "%<60%，已转人工复核，等待banker审核。【模拟】",
                        "GUARANTEE", app.getId());
            }
        }
        applicationMapper.updateById(app);

        log.info("[AI复审] 申请编号={}, 结果={}, 置信度={}, 命中规则={}, 流转至={}",
                app.getApplyNo(), result, score, hitRules, app.getApplyStatus());

        BizHouse h = houseMapper.selectById(app.getHouseId());
        BizRentalContract c = contractMapper.selectById(app.getContractId());
        BizGuarantee g = guaranteeMapper.selectOne(
                new LambdaQueryWrapper<BizGuarantee>().eq(BizGuarantee::getApplicationId, app.getId()));
        return toApplicationVO(app, h, c, g);
    }

    // ============================================================
    //  G-3-补 人工复审（banker 对 MANUAL_REVIEW 状态的申请做出裁决）
    // ============================================================

    /**
     * banker 人工复审保函申请
     * <p>
     * 对 AI 复审转人工（MANUAL_REVIEW）的申请做出裁决：
     * - APPROVED → 流转至 PENDING_PAY，租客可缴费
     * - REJECTED → 终止申请
     *
     * @param currentUserId 当前操作 banker ID
     * @param applicationId 申请 ID
     * @param decision      APPROVED 或 REJECTED
     * @param rejectReason  拒绝原因（REJECTED 时必填）
     */
    @Transactional(rollbackFor = Exception.class)
    public GuaranteeApplicationVO manualReviewApplication(Long currentUserId, Long applicationId,
                                                           String decision, String rejectReason) {
        BizGuaranteeApplication app = getApplication(applicationId);

        // 状态校验
        if (!STATUS_MANUAL_REVIEW.equals(app.getApplyStatus())) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET,
                    "当前申请状态为" + statusName(app.getApplyStatus()) + "，不可人工复审");
        }

        if ("APPROVED".equals(decision)) {
            // 人工复审通过，流转至待缴费
            app.setApplyStatus(STATUS_PENDING_PAY);
            app.setAiReviewDetail((app.getAiReviewDetail() != null ? app.getAiReviewDetail() : "")
                    + " | banker(" + currentUserId + ")人工复审通过，放行至待缴费。【模拟】");
            applicationMapper.updateById(app);
            log.info("[人工复审-通过] 申请编号={}, banker={}", app.getApplyNo(), currentUserId);
        } else if ("REJECTED".equals(decision)) {
            // 人工复审拒绝
            app.setApplyStatus(STATUS_REJECTED);
            app.setRejectReason(rejectReason != null ? rejectReason : "人工复审未通过");
            applicationMapper.updateById(app);
            log.info("[人工复审-拒绝] 申请编号={}, banker={}, 原因={}", app.getApplyNo(), currentUserId, rejectReason);
        } else {
            throw new BizException(ErrorCode.PARAM_ERROR, "复审结论只能为 APPROVED 或 REJECTED");
        }

        return toApplicationVO(app);
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
    /**
     * G-4 创建保函费支付订单（收银台支付成功后自动开立电子保函）
     */
    public PayOrderVO createPayOrder(Long currentUserId, Long applicationId) {
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

        // 生成保函费支付订单（待支付，收银台支付）
        return payService.createBizOrder(currentUserId, PayService.BIZ_GUARANTEE_FEE, app.getId(),
                "保函费（申请号" + app.getApplyNo() + "）", app.getGuaranteeFee(), null);
    }

    /**
     * 支付成功事件监听：保函费支付成功后开立电子保函
     */
    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void onPaySuccess(PaySuccessEvent event) {
        if (!PayService.BIZ_GUARANTEE_FEE.equals(event.getBizType()) || event.getBizId() == null) {
            return;
        }
        log.info("[保函缴费回调] 订单={}, 申请={}, 金额={}（模拟）", event.getOrderNo(), event.getBizId(), event.getAmount());
        issueGuarantee(event.getBizId());
    }

    /**
     * 开立电子保函（缴费成功后的业务主体）
     */
    @Transactional(rollbackFor = Exception.class)
    public GuaranteeVO issueGuarantee(Long applicationId) {
        BizGuaranteeApplication app = getApplication(applicationId);
        if (!STATUS_PENDING_PAY.equals(app.getApplyStatus())) {
            log.info("[保函开函] 申请={} 非待缴费状态，跳过（可能已开函）", applicationId);
            return null;
        }

        BizRentalContract contract = contractMapper.selectById(app.getContractId());

        // 模拟开函
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

        // 站内信：保函已开立
        sendInternalMessage(app.getTenantId(), "电子保函已开立",
                "保函申请（编号" + app.getApplyNo() + "）缴费成功，电子保函已开立。保函编号：" + guarantee.getGuaranteeNo()
                        + "，保函金额¥" + guarantee.getGuaranteeAmount() + "，有效期至" + guarantee.getExpireDate() + "。【模拟】",
                "GUARANTEE", guarantee.getId());

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

    /**
     * 房东名下保函列表（G-6 索赔入口：房东选择已开立保函发起索赔）
     * 仅返回当前用户作为房东名下的保函，租客视角无此接口
     */
    public List<GuaranteeVO> listLandlordGuarantees(Long currentUserId) {
        BizLandlord landlord = landlordMapper.selectOne(
                new LambdaQueryWrapper<BizLandlord>().eq(BizLandlord::getUserId, currentUserId));
        if (landlord == null) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "当前用户不是房东，无名下保函");
        }
        List<BizGuarantee> guarantees = guaranteeMapper.selectList(
                new LambdaQueryWrapper<BizGuarantee>()
                        .eq(BizGuarantee::getLandlordId, landlord.getId())
                        .orderByDesc(BizGuarantee::getCreateTime));
        return guarantees.stream()
                .map(g -> toGuaranteeVO(g, applicationMapper.selectById(g.getApplicationId())))
                .toList();
    }

    /**
     * 租客名下已开立保函列表（退租留档选函用）
     */
    public List<GuaranteeVO> listTenantGuarantees(Long tenantId) {
        List<BizGuarantee> guarantees = guaranteeMapper.selectList(
                new LambdaQueryWrapper<BizGuarantee>()
                        .inSql(BizGuarantee::getApplicationId,
                                "SELECT id FROM biz_guarantee_application WHERE tenant_id = " + tenantId)
                        .orderByDesc(BizGuarantee::getCreateTime));
        return guarantees.stream()
                .map(g -> toGuaranteeVO(g, applicationMapper.selectById(g.getApplicationId())))
                .toList();
    }

    /**
     * 管理端：待房东确认的退租留档列表（照片合格但房东未确认无需索赔）
     */
    public IPage<Map<String, Object>> pagePendingLandlordConfirm(int pageNum, int pageSize) {
        checkBackOffice();
        Page<BizMoveoutRecord> page = moveoutRecordMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<BizMoveoutRecord>()
                        .eq(BizMoveoutRecord::getCheckResult, "PASS")
                        .ne(BizMoveoutRecord::getLandlordConfirm, "CONFIRMED")
                        .orderByDesc(BizMoveoutRecord::getCreateTime));
        return page.convert(this::toPendingConfirmVO);
    }

    /**
     * 管理端：代房东确认无需索赔（留档仅系统防纠纷，房东确认不索赔才是完美结束）
     */
    @Transactional
    public Map<String, Object> landlordConfirmMoveout(Long recordId, String remark) {
        checkBackOffice();
        BizMoveoutRecord record = moveoutRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "留档记录不存在");
        }
        if (!"PASS".equals(record.getCheckResult())) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "仅照片审核合格的留档可确认无需索赔");
        }
        record.setLandlordConfirm("CONFIRMED");
        record.setLandlordConfirmTime(LocalDateTime.now());
        record.setLandlordConfirmRemark(remark);
        moveoutRecordMapper.updateById(record);
        Map<String, Object> m = new HashMap<>();
        m.put("recordId", record.getId());
        m.put("recordNo", record.getRecordNo());
        m.put("guaranteeNo", record.getGuaranteeNo());
        m.put("landlordConfirm", "CONFIRMED");
        m.put("message", "已代房东确认无需索赔（模拟），该保函房屋状态更新为：已确认（完美结束）");
        return m;
    }

    private Map<String, Object> toPendingConfirmVO(BizMoveoutRecord r) {
        Map<String, Object> m = new HashMap<>();
        m.put("recordId", r.getId());
        m.put("recordNo", r.getRecordNo());
        m.put("guaranteeId", r.getGuaranteeId());
        m.put("guaranteeNo", r.getGuaranteeNo());
        m.put("photos", r.getPhotosJson());
        m.put("checkResult", r.getCheckResult());
        m.put("checkDetail", r.getCheckDetail());
        m.put("landlordConfirm", r.getLandlordConfirm());
        m.put("createTime", r.getCreateTime());
        BizGuarantee g = guaranteeMapper.selectById(r.getGuaranteeId());
        if (g != null) {
            m.put("guaranteeAmount", g.getGuaranteeAmount());
            BizGuaranteeApplication app = applicationMapper.selectById(g.getApplicationId());
            if (app != null) {
                m.put("tenantName", app.getApplicantName());
                m.put("landlordName", app.getLandlordName());
                BizRentalContract contract = contractMapper.selectById(app.getContractId());
                if (contract != null) {
                    m.put("rentStartDate", contract.getRentStartDate());
                    m.put("rentEndDate", contract.getRentEndDate());
                }
            }
            BizHouse house = houseMapper.selectById(g.getHouseId());
            if (house != null) {
                m.put("houseAddress", house.getAddress());
            }
        }
        return m;
    }

    // ============================================================
    //  工具方法
    // ============================================================

    private void checkBackOffice() {
        String role = UserContext.getRole();
        if (!"ADMIN".equals(role) && !"BANK_OPERATOR".equals(role)) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅银行运营人员可操作");
        }
    }

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

    // ============================================================
    //  退租留档：结束租房上传房屋照片，AI 合格审核（模拟）
    // ============================================================

    /**
     * 退租留档提交
     * <p>
     * 租客结束租房时上传房屋照片，系统做照片合格审核（模拟）：
     * 照片 &gt;= 3 张且画面清晰（文件名不含"模糊/遮挡/反光/不清晰"）→ PASS 合格留档；
     * 否则 → REVIEW（需补拍后重新提交或转人工复核）。
     */
    @Transactional
    public Map<String, Object> submitMoveoutRecord(Long currentUserId, MoveoutRecordDTO dto) {
        BizGuarantee guarantee = guaranteeMapper.selectById(dto.getGuaranteeId());
        if (guarantee == null) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "保函不存在");
        }
        // 越权校验：仅该保函对应的租客可提交退租留档
        BizGuaranteeApplication app = applicationMapper.selectById(guarantee.getApplicationId());
        if (app == null || !app.getTenantId().equals(currentUserId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅该保函对应的租客可提交退租留档");
        }

        // 解析照片列表（JSON 数组）
        List<String> photos;
        try {
            photos = JSONUtil.toList(dto.getPhotos(), String.class);
        } catch (Exception e) {
            throw new BizException(ErrorCode.PARAM_ERROR, "照片列表格式不正确");
        }
        if (photos == null || photos.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "请上传房屋照片");
        }

        // AI 照片合格审核（模拟）
        boolean blurHit = photos.stream().anyMatch(f -> f != null && (f.contains("模糊") || f.contains("遮挡")
                || f.contains("反光") || f.contains("不清晰")));
        String result;
        String detail;
        if (photos.size() >= 3 && !blurHit) {
            result = "PASS";
            detail = "共上传房屋照片" + photos.size() + "张，画面清晰、无遮挡，审核合格，已留档。【模拟】";
        } else {
            result = "REVIEW";
            StringBuilder tip = new StringBuilder("共上传房屋照片" + photos.size() + "张；");
            if (photos.size() < 3) {
                tip.append("照片数量不足 3 张；");
            }
            if (blurHit) {
                tip.append("存在画面模糊/遮挡照片；");
            }
            tip.append("未通过合格审核，需补拍后重新提交或转人工复核。【模拟】");
            detail = tip.toString();
        }

        BizMoveoutRecord record = new BizMoveoutRecord();
        record.setRecordNo(generateNo("TZ"));
        record.setGuaranteeId(guarantee.getId());
        record.setGuaranteeNo(guarantee.getGuaranteeNo());
        record.setTenantId(currentUserId);
        record.setLandlordId(guarantee.getLandlordId());
        record.setHouseId(app.getHouseId());
        record.setPhotosJson(dto.getPhotos());
        record.setCheckResult(result);
        record.setCheckDetail(detail);
        record.setRemark(dto.getRemark());
        moveoutRecordMapper.insert(record);

        log.info("[退租留档] 编号={}, 保函={}, 照片={}张, 审核结果={}", record.getRecordNo(),
                guarantee.getGuaranteeNo(), photos.size(), result);
        return toMoveoutVO(record);
    }

    /**
     * 我的退租留档记录（分页，租客本人）
     */
    public IPage<Map<String, Object>> pageMoveoutRecords(Long currentUserId, int pageNum, int pageSize) {
        Page<BizMoveoutRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BizMoveoutRecord> qw = new LambdaQueryWrapper<BizMoveoutRecord>()
                .eq(BizMoveoutRecord::getTenantId, currentUserId)
                .orderByDesc(BizMoveoutRecord::getCreateTime);
        return moveoutRecordMapper.selectPage(page, qw).convert(this::toMoveoutVO);
    }

    private Map<String, Object> toMoveoutVO(BizMoveoutRecord r) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", r.getId());
        m.put("recordNo", r.getRecordNo());
        m.put("guaranteeId", r.getGuaranteeId());
        m.put("guaranteeNo", r.getGuaranteeNo());
        m.put("landlordId", r.getLandlordId());
        m.put("houseId", r.getHouseId());
        m.put("photos", r.getPhotosJson());
        m.put("checkResult", r.getCheckResult());
        m.put("checkDetail", r.getCheckDetail());
        m.put("remark", r.getRemark());
        m.put("createTime", r.getCreateTime());
        return m;
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
        // 房屋租住情况：租期前/中/后 × 被索赔/正常 × 留档确认无需索赔
        String[] sit = houseSituation(guarantee, app);
        vo.setHouseSituation(sit[0]);
        vo.setHouseSituationName(sit[1]);
        // 租期（关联租赁合同）
        if (app != null && app.getContractId() != null) {
            BizRentalContract contract = contractMapper.selectById(app.getContractId());
            if (contract != null) {
                vo.setRentStartDate(contract.getRentStartDate());
                vo.setRentEndDate(contract.getRentEndDate());
            }
        }
        return vo;
    }

    /**
     * 派生房屋租住情况（状态名不携带阶段前缀，阶段由前端分区标题体现）：
     * - 租期前（未入住）：待入住
     * - 租期中：正常 / 被索赔
     * - 租期后（仅提交留档并通过审核才归入）：待确认 / 已确认 / 已过期 / 被索赔
     *   有索赔 → 被索赔；无合格留档（未提交或 REVIEW）→ 不归入分区（返回 null）；
     *   保函过期（expireDate < 今日）→ 已过期；房东未确认 → 待确认；已确认 → 已确认。
     * 租期取自关联租赁合同 rent_end_date（保函有效期长于租期，租期结束后保函仍有效属正常）。
     */
    private String[] houseSituation(BizGuarantee guarantee, BizGuaranteeApplication app) {
        boolean hasClaim = claimMapper.selectCount(new LambdaQueryWrapper<BizGuaranteeClaim>()
                .eq(BizGuaranteeClaim::getGuaranteeId, guarantee.getId())
                .ne(BizGuaranteeClaim::getClaimStatus, "REJECTED")) > 0;
        // 留档仅系统防纠纷；租后需房东（管理端代）确认不索赔才算完美结束
        boolean hasPass = moveoutRecordMapper.selectCount(new LambdaQueryWrapper<BizMoveoutRecord>()
                .eq(BizMoveoutRecord::getGuaranteeId, guarantee.getId())
                .eq(BizMoveoutRecord::getCheckResult, "PASS")) > 0;
        boolean hasConfirm = moveoutRecordMapper.selectCount(new LambdaQueryWrapper<BizMoveoutRecord>()
                .eq(BizMoveoutRecord::getGuaranteeId, guarantee.getId())
                .eq(BizMoveoutRecord::getCheckResult, "PASS")
                .eq(BizMoveoutRecord::getLandlordConfirm, "CONFIRMED")) > 0;
        LocalDate today = LocalDate.now();
        LocalDate rentStart = null;
        LocalDate rentEnd = null;
        if (app != null && app.getContractId() != null) {
            BizRentalContract contract = contractMapper.selectById(app.getContractId());
            if (contract != null) {
                rentStart = contract.getRentStartDate();
                rentEnd = contract.getRentEndDate();
            }
        }
        if (rentEnd == null) {
            return new String[]{null, null};
        }
        // 租期前：租期未开始（保函已开立，待入住）
        if (rentStart != null && rentStart.isAfter(today)) {
            return new String[]{SIT_PRE_RENTAL, "待入住"};
        }
        boolean ended = rentEnd.isBefore(today);
        if (hasClaim) {
            return ended
                    ? new String[]{SIT_ENDED_CLAIMED, "被索赔"}
                    : new String[]{SIT_RENTING_CLAIMED, "被索赔"};
        }
        if (ended) {
            // 只有提交留档并通过审核才归入租期后分区
            if (!hasPass) {
                return new String[]{null, null};
            }
            // 保函到期未发起索赔 → 已过期（不可再发起索赔）
            if (guarantee.getExpireDate() != null && guarantee.getExpireDate().isBefore(today)) {
                return new String[]{SIT_ENDED_EXPIRED, "已过期"};
            }
            // 留档合格后，需房东确认不索赔才算完美结束
            return hasConfirm
                    ? new String[]{SIT_ENDED_CONFIRMED, "已确认"}
                    : new String[]{SIT_ENDED_PENDING_CONFIRM, "待确认"};
        }
        return new String[]{SIT_RENTING_NORMAL, "正常"};
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
}
