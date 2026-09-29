package com.icbc.qingqi.module.admin.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * B转A观察期用户列表 VO（管理端展示）
 * <p>
 * 贷款数据与用户画像关联：B 类授信在观察期内的用户，展示观察状态/评分/转A进度，
 * 便于银行端跟踪 B 转 A 全流程。
 */
@Data
public class ObservationUserVO {
    /** 用户ID */
    private Long userId;
    /** 用户名 */
    private String username;
    /** 真实姓名 */
    private String realName;
    /** 手机号 */
    private String phone;
    /** 授信类型（恒为 B_TYPE） */
    private String creditType;
    /** 总额度 */
    private BigDecimal totalLimit;
    /** 已用额度 */
    private BigDecimal usedLimit;
    /** 可用额度 */
    private BigDecimal availableLimit;
    /** 观察期状态：OBSERVING/PROMOTED/EXITED */
    private String observationStatus;
    /** 观察月数 */
    private Integer observationMonths;
    /** 观察期累计评分 */
    private Integer observationScore;
    /** 转A进度百分比 */
    private Integer promotionProgress;
    /** 授信创建时间 */
    private LocalDateTime createTime;
}
