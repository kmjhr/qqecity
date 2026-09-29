package com.icbc.qingqi.module.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 注册AI审核记录实体（模拟）
 * <p>
 * 对应表：biz_registration_review
 * 记录每次注册申请的 AI 审核明细：白名单人群 / 同一材料同一人 / 重复注册
 */
@Data
@TableName("biz_registration_review")
public class BizRegistrationReview {

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 审核编号（REG+时间戳） */
    private String reviewNo;

    /** 申请用户名 */
    private String username;

    /** 真实姓名 */
    private String realName;

    /** 身份证号（SHA-256 哈希，不存明文） */
    private String idCard;

    /** 手机号 */
    private String phone;

    /** 人群类型 */
    private String userType;

    /** 学校 */
    private String school;

    /** 学历层次：UNDERGRADUATE/MASTER/DOCTOR */
    private String educationLevel;

    /** 毕业日期 */
    private LocalDate graduationDate;

    /** 核验方式：XUE_XIN_WANG学信网/STUDENT_CARD学生证·仅在校生/GRAD_CERT毕业证·仅毕业2年内 */
    private String verifyType;

    /** 学信档案验证码/学号（模拟） */
    private String studentNo;

    /** 证件照片（base64，毕业证/学生证，模拟识别上传） */
    private String certPhoto;

    /** 白名单人群审核：1通过 0拒绝 */
    private Integer whitelistPass;

    /** 白名单审核明细 */
    private String whitelistDetail;

    /** 同一材料/同一人审核：1通过 0命中重复 */
    private Integer materialPass;

    /** 同一材料/同一人审核明细 */
    private String materialDetail;

    /** 结论：APPROVED/REJECTED */
    private String result;

    /** 拒绝原因 */
    private String rejectReason;

    /** 注册成功后的用户ID（拒绝为空） */
    private Long userId;

    /** 逻辑删除：0-存在，1-删除 */
    @TableLogic
    private Integer deleted;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
