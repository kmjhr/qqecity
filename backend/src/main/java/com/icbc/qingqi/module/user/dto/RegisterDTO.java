package com.icbc.qingqi.module.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 用户注册 DTO
 * <p>
 * 新增字段（realName/idCard/school/educationLevel/graduationDate/verifyType/studentNo）
 * 供「AI 智能审核（模拟）」使用：白名单人群核验 + 学历核验 + 同一材料/同一人/重复注册查重。
 * 学历相关字段均为可选（兼容旧调用），但人群类型为 STUDENT/GRADUATE 时将被 AI 审核强制校验。
 */
@Data
public class RegisterDTO {

    /** 用户名 / 手机号 */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度 3-20 位")
    private String username;

    /** 密码 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度 6-20 位")
    private String password;

    /** 昵称 */
    private String nickname;

    /** 手机号 */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 邮箱 */
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 人群类型
     * STUDENT-在校生 / GRADUATE-毕业2年内 / ENTREPRENEUR-青年创业者 / OTHER-其他青年（非白名单，拒绝注册）
     */
    private String userType;

    /** 真实姓名（实名信息，AI 审核用） */
    private String realName;

    /** 身份证号（AI 审核同一人查重用；审核记录仅存 SHA-256 哈希） */
    private String idCard;

    /** 学校（学历核验：须命中启用高校库） */
    private String school;

    /** 学历层次：UNDERGRADUATE-本科 / MASTER-硕士 / DOCTOR-博士 */
    private String educationLevel;

    /** 毕业日期（GRADUATE 需毕业2年内，STUDENT 需在校） */
    private LocalDate graduationDate;

    /** 核验方式：XUE_XIN_WANG-学信网在线核验（模拟）/ STUDENT_CARD-学生证照片识别·仅在校生（模拟）/ GRAD_CERT-毕业证照片识别·仅毕业2年内（模拟） */
    private String verifyType;

    /** 学信档案验证码/学号（模拟） */
    private String studentNo;
}
