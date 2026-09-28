package com.icbc.qingqi.module.user.dto;

import lombok.Data;

/**
 * 学生证照片 AI 识别结果 VO（模拟）
 */
@Data
public class StudentCardOcrVO {

    /** 识别出的学校 */
    private String school;

    /** 识别出的学号 */
    private String studentNo;

    /** 识别置信度（0-100，模拟） */
    private Integer confidence;

    /** 是否模拟 */
    private Boolean simulated = Boolean.TRUE;

    /** 识别提示文案 */
    private String message;
}
