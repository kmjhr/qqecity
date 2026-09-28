package com.icbc.qingqi.module.user.dto;

import lombok.Data;

/**
 * 学生证照片 AI 识别请求 DTO（模拟）
 * <p>
 * 演示用：可传图片 base64 或文件名占位，后端固定返回模拟识别结果（标注"模拟"）
 */
@Data
public class StudentCardOcrDTO {

    /** 学生证照片 base64（模拟，可不传） */
    private String imageBase64;

    /** 文件名（模拟，可不传） */
    private String fileName;

    /** 演示用的学校名（如传该字段则原样返回，便于演示"识别"效果） */
    private String demoSchool;

    /** 演示用的学号（如传该字段则原样返回） */
    private String demoStudentNo;
}
