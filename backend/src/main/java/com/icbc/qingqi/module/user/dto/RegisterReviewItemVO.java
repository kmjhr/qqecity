package com.icbc.qingqi.module.user.dto;

import lombok.Data;

/**
 * 注册 AI 审核单项结果（模拟）
 * <p>
 * 用于前端逐项展示：白名单人群 / 学历核验 / 同一材料同一人 / 重复注册
 */
@Data
public class RegisterReviewItemVO {

    /** 审核项标识：WHITELIST_CROWD / EDUCATION / SAME_PERSON / SAME_MATERIAL / DUPLICATE_USERNAME */
    private String code;

    /** 审核项名称：白名单人群 / 学历核验 / 同一人 / 同一材料 / 重复注册 */
    private String name;

    /** 是否通过：true-通过，false-拒绝 */
    private Boolean pass;

    /** 审核明细（通过原因 / 拒绝原因） */
    private String detail;
}
