package com.icbc.qingqi.module.user.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icbc.qingqi.common.BizException;
import com.icbc.qingqi.common.ErrorCode;
import com.icbc.qingqi.module.message.entity.SysMessage;
import com.icbc.qingqi.module.message.mapper.SysMessageMapper;
import com.icbc.qingqi.module.user.dto.*;
import com.icbc.qingqi.module.user.entity.BizRegistrationReview;
import com.icbc.qingqi.module.user.entity.BizSchool;
import com.icbc.qingqi.module.user.entity.SysUser;
import com.icbc.qingqi.module.user.mapper.BizRegistrationReviewMapper;
import com.icbc.qingqi.module.user.mapper.BizSchoolMapper;
import com.icbc.qingqi.module.user.mapper.SysUserMapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 注册 AI 审核服务（模拟规则引擎）
 * <p>
 * 对应《青启e城》注册流程「AI 智能审核」，三项审核（全部为演示规则，标注"模拟"）：
 * <ol>
 *   <li>白名单人群：人群类型 ∈ {STUDENT 在校生 / GRADUATE 毕业2年内 / ENTREPRENEUR 青年创业者}；
 *       STUDENT/GRADUATE 需学历核验：学校 ∈ biz_school 高校库（启用）+ 学历层次合法 +
 *       毕业日期（GRADUATE 毕业2年内 / STUDENT 在校）+ 核验方式有效（学信网在线核验或学生证照片识别，模拟）</li>
 *   <li>同一材料/同一人：身份证号已注册 → 同一人；手机号已注册 → 同一材料</li>
 *   <li>重复注册：用户名 / 证件号 / 手机号三重查重</li>
 * </ol>
 * 不接入真实征信/学信网，全部为服务层规则模拟。
 */
@Service
public class RegistrationReviewService {

    /** 白名单人群（与《工行杯9.26.docx》「人群白名单：在校大学生、毕业2年内应届生」一致，青年创业者纳入扶持人群） */
    private static final Set<String> WHITELIST_CROWDS = Set.of("STUDENT", "GRADUATE", "ENTREPRENEUR");

    /** 合法学历层次 */
    private static final Set<String> EDUCATION_LEVELS = Set.of("UNDERGRADUATE", "MASTER", "DOCTOR");

    /** 合法核验方式：学信网在线核验 / 学生证照片识别（仅在校生）/ 毕业证照片识别（仅毕业2年内） */
    private static final Set<String> VERIFY_TYPES = Set.of("XUE_XIN_WANG", "STUDENT_CARD", "GRAD_CERT");

    /** 毕业 2 年内（730 天） */
    private static final long GRADUATE_WITHIN_DAYS = 730L;

    private final SysUserMapper userMapper;
    private final BizSchoolMapper schoolMapper;
    private final BizRegistrationReviewMapper reviewMapper;
    private final SysMessageMapper messageMapper;

    /** 自引用代理（@Lazy 自注入，保证 REQUIRES_NEW 事务切面生效） */
    @Lazy
    private final RegistrationReviewService self;

    public RegistrationReviewService(SysUserMapper userMapper,
                                     BizSchoolMapper schoolMapper,
                                     BizRegistrationReviewMapper reviewMapper,
                                     SysMessageMapper messageMapper,
                                     @Lazy RegistrationReviewService self) {
        this.userMapper = userMapper;
        this.schoolMapper = schoolMapper;
        this.reviewMapper = reviewMapper;
        this.messageMapper = messageMapper;
        this.self = self;
    }

    // ============================================================
    //  AI 审核规则引擎（模拟）
    // ============================================================

    /**
     * 执行 AI 审核（不落库、不创建账号），供预审接口 /auth/register/ai-review 使用
     */
    public RegisterReviewVO review(RegisterDTO dto) {
        List<RegisterReviewItemVO> items = new ArrayList<>();
        boolean whitelistPass = checkWhitelist(dto, items);
        boolean materialPass = checkDuplicates(dto, items);
        boolean passed = whitelistPass && materialPass;

        RegisterReviewVO vo = new RegisterReviewVO();
        vo.setReviewNo(generateReviewNo());
        vo.setPassed(passed);
        vo.setItems(items);
        if (!passed) {
            vo.setRejectReason(buildRejectReason(items));
        }
        return vo;
    }

    /**
     * 执行 AI 审核，拒绝时留痕并抛出对应错误码：
     * 重复注册（用户名/证件号/手机号）→ 2002 用户已存在；其余（非白名单等）→ 3001 业务规则不满足
     * <p>
     * 说明：拒绝的注册申请同样写入审核记录（result=REJECTED，user_id 为空），保证审核留痕可追溯
     */
    public RegisterReviewVO reviewOrThrow(RegisterDTO dto) {
        RegisterReviewVO vo = review(dto);
        if (!vo.getPassed()) {
            // 拒绝留痕：经代理调用 REQUIRES_NEW 独立事务提交，避免被 register 外层事务回滚
            self.persistRejected(dto, vo);
            boolean duplicate = vo.getItems().stream().anyMatch(i -> !Boolean.TRUE.equals(i.getPass())
                    && ("DUPLICATE_USERNAME".equals(i.getCode())
                    || "SAME_PERSON".equals(i.getCode())
                    || "SAME_MATERIAL".equals(i.getCode())));
            throw new BizException(duplicate ? ErrorCode.USER_ALREADY_EXISTS : ErrorCode.BIZ_RULE_NOT_MET, vo.getRejectReason());
        }
        return vo;
    }

    /**
     * 拒绝的注册申请留痕（独立事务提交，不受外层注册事务回滚影响）
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistRejected(RegisterDTO dto, RegisterReviewVO vo) {
        persist(dto, vo, null);
    }

    /**
     * 审核记录落库（注册成功或失败均留痕）
     */
    public void persist(RegisterDTO dto, RegisterReviewVO vo, Long userId) {
        BizRegistrationReview record = new BizRegistrationReview();
        record.setReviewNo(vo.getReviewNo());
        record.setUsername(dto.getUsername());
        record.setRealName(dto.getRealName());
        record.setIdCard(hashIdCard(dto.getIdCard()));
        record.setPhone(dto.getPhone());
        record.setUserType(dto.getUserType());
        record.setSchool(dto.getSchool());
        record.setEducationLevel(dto.getEducationLevel());
        record.setGraduationDate(dto.getGraduationDate());
        record.setVerifyType(dto.getVerifyType());
        record.setStudentNo(dto.getStudentNo());
        record.setWhitelistPass(vo.getItems().stream()
                .filter(i -> "WHITELIST_CROWD".equals(i.getCode()) || "EDUCATION".equals(i.getCode()))
                .allMatch(i -> Boolean.TRUE.equals(i.getPass())) ? 1 : 0);
        record.setWhitelistDetail(joinDetail(vo, "WHITELIST_CROWD", "EDUCATION"));
        record.setMaterialPass(vo.getItems().stream()
                .filter(i -> "SAME_PERSON".equals(i.getCode()) || "SAME_MATERIAL".equals(i.getCode()) || "DUPLICATE_USERNAME".equals(i.getCode()))
                .allMatch(i -> Boolean.TRUE.equals(i.getPass())) ? 1 : 0);
        record.setMaterialDetail(joinDetail(vo, "SAME_PERSON", "SAME_MATERIAL", "DUPLICATE_USERNAME"));
        record.setResult(Boolean.TRUE.equals(vo.getPassed()) ? "APPROVED" : "REJECTED");
        record.setRejectReason(vo.getRejectReason());
        record.setUserId(userId);
        reviewMapper.insert(record);
    }

    /**
     * 注册成功后发送欢迎站内信
     */
    public void sendWelcomeMessage(Long userId) {
        SysMessage msg = new SysMessage();
        msg.setUserId(userId);
        msg.setTitle("注册成功 · AI 智能审核通过");
        msg.setContent("欢迎加入青启e城！您的注册已通过 AI 智能审核（模拟）：白名单人群核验通过、未命中重复注册。可开始体验安居保函、青创e贷、预算储蓄等服务。");
        msg.setType("SYSTEM");
        msg.setBizType("REGISTER");
        msg.setIsRead(0);
        messageMapper.insert(msg);
    }

    // ============================================================
    //  高校库 / 学生证识别（模拟）
    // ============================================================

    /**
     * 高校库列表（仅启用），注册页学校下拉
     */
    public List<SchoolVO> listSchools() {
        return schoolMapper.selectList(new LambdaQueryWrapper<BizSchool>()
                        .eq(BizSchool::getStatus, 1)
                        .orderByAsc(BizSchool::getId))
                .stream().map(s -> {
                    SchoolVO vo = new SchoolVO();
                    vo.setId(s.getId());
                    vo.setSchoolName(s.getSchoolName());
                    vo.setSchoolCode(s.getSchoolCode());
                    return vo;
                }).toList();
    }

    /**
     * 学生证/毕业证照片 AI 识别（模拟）
     * <p>
     * 演示：未接真实 OCR，根据请求中的演示字段或默认值返回模拟识别结果，标注"模拟"
     */
    public StudentCardOcrVO mockOcr(StudentCardOcrDTO dto) {
        StudentCardOcrVO vo = new StudentCardOcrVO();
        vo.setSchool(dto.getDemoSchool() != null && !dto.getDemoSchool().isBlank()
                ? dto.getDemoSchool().trim()
                : "中山大学");
        vo.setStudentNo(dto.getDemoStudentNo() != null && !dto.getDemoStudentNo().isBlank()
                ? dto.getDemoStudentNo().trim()
                : "XH" + RandomUtil.randomNumbers(8));
        vo.setConfidence(98);
        vo.setSimulated(Boolean.TRUE);
        vo.setMessage("已模拟识别证件照片（演示），识别结果可直接回填注册表单");
        return vo;
    }

    // ============================================================
    //  私有规则方法
    // ============================================================

    /**
     * 白名单人群审核 + 学历核验（STUDENT/GRADUATE）
     */
    private boolean checkWhitelist(RegisterDTO dto, List<RegisterReviewItemVO> items) {
        String userType = dto.getUserType();
        if (userType == null || userType.isBlank()) {
            items.add(item("WHITELIST_CROWD", "白名单人群", false, "未选择人群类型，无法判断是否属于白名单人群"));
            return false;
        }
        if (!WHITELIST_CROWDS.contains(userType)) {
            items.add(item("WHITELIST_CROWD", "白名单人群", false,
                    "人群类型「" + userType + "」不在白名单人群范围（在校大学生/毕业2年内/青年创业者），暂不支持注册"));
            return false;
        }
        items.add(item("WHITELIST_CROWD", "白名单人群", true,
                "人群类型「" + userTypeName(userType) + "」属于白名单人群"));

        if ("STUDENT".equals(userType) || "GRADUATE".equals(userType)) {
            return checkEducation(dto, items);
        }
        // ENTREPRENEUR：青年创业者属白名单，创业计划资质在贷款预审环节核验，注册阶段仅做人群校验
        return true;
    }

    /**
     * 学历核验（模拟规则，对应注册流程图「③ AI学历审查」）
     */
    private boolean checkEducation(RegisterDTO dto, List<RegisterReviewItemVO> items) {
        boolean pass = true;
        StringBuilder detail = new StringBuilder("学历核验（模拟）：");

        // 1. 学校 ∈ biz_school 高校库（启用）
        String school = dto.getSchool();
        if (school == null || school.isBlank()) {
            detail.append("未填写学校；");
            pass = false;
        } else {
            Long cnt = schoolMapper.selectCount(new LambdaQueryWrapper<BizSchool>()
                    .eq(BizSchool::getSchoolName, school.trim())
                    .eq(BizSchool::getStatus, 1));
            if (cnt != null && cnt > 0) {
                detail.append("学校「").append(school.trim()).append("」∈高校库（启用）；");
            } else {
                detail.append("学校「").append(school.trim()).append("」不在启用高校库，核验不通过；");
                pass = false;
            }
        }

        // 2. 学历层次合法
        String level = dto.getEducationLevel();
        if (level == null || !EDUCATION_LEVELS.contains(level)) {
            detail.append("学历层次不合法（需本科/硕士/博士）；");
            pass = false;
        } else {
            detail.append("学历层次合法（").append(levelName(level)).append("）；");
        }

        // 3. 毕业日期（GRADUATE 毕业2年内 / STUDENT 在校）
        LocalDate grad = dto.getGraduationDate();
        LocalDate today = LocalDate.now();
        if ("GRADUATE".equals(dto.getUserType())) {
            if (grad == null) {
                detail.append("缺少毕业日期；");
                pass = false;
            } else if (grad.isAfter(today)) {
                detail.append("毕业日期晚于当前日期，不符合毕业2年内；");
                pass = false;
            } else {
                long days = ChronoUnit.DAYS.between(grad, today);
                if (days > GRADUATE_WITHIN_DAYS) {
                    detail.append("毕业已超2年（").append(days / 365).append("年），不在白名单范围；");
                    pass = false;
                } else {
                    detail.append("毕业日期").append(grad).append("（毕业").append(days / 365 < 1 ? "1年内" : days / 365 + "年内").append("）符合毕业2年内；");
                }
            }
        } else {
            if (grad == null) {
                detail.append("在校生缺少毕业日期；");
                pass = false;
            } else if (grad.isBefore(today)) {
                detail.append("毕业日期早于当前日期，不符合在校生身份；");
                pass = false;
            } else {
                detail.append("毕业日期").append(grad).append("（在校期间）符合在校生身份；");
            }
        }

        // 4. 核验方式有效（学信网在线核验 / 学生证照片识别·仅在校生 / 毕业证照片识别·仅毕业2年内，均为模拟）
        String verifyType = dto.getVerifyType();
        String studentNo = dto.getStudentNo();
        if (verifyType == null || !VERIFY_TYPES.contains(verifyType) || studentNo == null || studentNo.isBlank()) {
            detail.append("缺少核验方式或学信档案验证码/学号；");
            pass = false;
        } else if ("STUDENT_CARD".equals(verifyType) && "GRADUATE".equals(dto.getUserType())) {
            detail.append("核验方式「学生证照片识别」不适用于毕业人群，毕业须通过「学信网在线核验」或「毕业证照片识别」；");
            pass = false;
        } else if ("GRAD_CERT".equals(verifyType) && !"GRADUATE".equals(dto.getUserType())) {
            detail.append("核验方式「毕业证照片识别」仅限毕业2年内人群，在校生请使用「学信网在线核验」或「学生证照片识别」；");
            pass = false;
        } else {
            String verifyName = switch (verifyType) {
                case "XUE_XIN_WANG" -> "学信网在线核验（模拟）";
                case "STUDENT_CARD" -> "学生证照片识别（模拟）";
                case "GRAD_CERT" -> "毕业证照片识别（模拟）";
                default -> verifyType;
            };
            detail.append("核验方式「").append(verifyName).append("」核验通过；");
        }

        items.add(item("EDUCATION", "学历核验", pass, detail.toString()));
        return pass;
    }

    /**
     * 同一材料/同一人/重复注册审核
     */
    private boolean checkDuplicates(RegisterDTO dto, List<RegisterReviewItemVO> items) {
        boolean pass = true;

        // 重复注册：用户名
        if (dto.getUsername() != null && !dto.getUsername().isBlank()) {
            Long c = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, dto.getUsername().trim()));
            if (c != null && c > 0) {
                items.add(item("DUPLICATE_USERNAME", "重复注册", false,
                        "用户名「" + dto.getUsername().trim() + "」已注册，请直接登录"));
                pass = false;
            }
        }

        // 同一人：身份证号
        if (dto.getIdCard() != null && !dto.getIdCard().isBlank()) {
            Long c = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getIdCard, dto.getIdCard().trim()));
            if (c != null && c > 0) {
                items.add(item("SAME_PERSON", "同一人", false,
                        "该身份证号已注册（同一人重复注册），请直接登录"));
                pass = false;
            }
        }

        // 同一材料：手机号
        if (dto.getPhone() != null && !dto.getPhone().isBlank()) {
            Long c = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getPhone, dto.getPhone().trim()));
            if (c != null && c > 0) {
                items.add(item("SAME_MATERIAL", "同一材料", false,
                        "该手机号已注册（同一材料/联系方式），请直接登录"));
                pass = false;
            }
        }

        if (pass) {
            items.add(item("SAME_PERSON", "同一材料/同一人", true,
                    "身份证号、手机号均未注册，未命中重复注册"));
        }
        return pass;
    }

    // ============================================================
    //  工具方法
    // ============================================================

    private RegisterReviewItemVO item(String code, String name, boolean pass, String detail) {
        RegisterReviewItemVO vo = new RegisterReviewItemVO();
        vo.setCode(code);
        vo.setName(name);
        vo.setPass(pass);
        vo.setDetail(detail);
        return vo;
    }

    private String buildRejectReason(List<RegisterReviewItemVO> items) {
        StringBuilder sb = new StringBuilder("AI 智能审核未通过（模拟）：");
        items.stream().filter(i -> !Boolean.TRUE.equals(i.getPass()))
                .forEach(i -> sb.append("【").append(i.getName()).append("】").append(i.getDetail()).append(" "));
        return sb.toString().trim();
    }

    private String joinDetail(RegisterReviewVO vo, String... codes) {
        return vo.getItems().stream()
                .filter(i -> List.of(codes).contains(i.getCode()))
                .map(RegisterReviewItemVO::getDetail)
                .reduce((a, b) -> a + "；" + b)
                .orElse(null);
    }

    private String generateReviewNo() {
        return "REG" + DateUtil.format(LocalDateTime.now(), "yyyyMMddHHmmss") + RandomUtil.randomNumbers(4);
    }

    private String hashIdCard(String idCard) {
        if (idCard == null || idCard.isBlank()) {
            return null;
        }
        return cn.hutool.crypto.SecureUtil.sha256(idCard.trim());
    }

    private String userTypeName(String userType) {
        return switch (userType) {
            case "STUDENT" -> "在校大学生";
            case "GRADUATE" -> "毕业2年内青年";
            case "ENTREPRENEUR" -> "青年创业者";
            default -> userType;
        };
    }

    private String levelName(String level) {
        return switch (level) {
            case "UNDERGRADUATE" -> "本科";
            case "MASTER" -> "硕士";
            case "DOCTOR" -> "博士";
            default -> level;
        };
    }
}
