package com.icbc.qingqi.module.safety.dto;

import lombok.Data;

import java.util.List;

/**
 * 反诈情景教学提交结果 VO
 * <p>
 * 选错有纠偏文案；进度可续（biz_fraud_detection_log 持久化记录）
 */
@Data
public class ScenarioSubmitVO {

    /** 内容 ID */
    private Long scenarioId;

    /** 总题数 */
    private Integer totalQuestions;

    /** 答对题数 */
    private Integer correctCount;

    /** 正确率（%） */
    private Integer accuracy;

    /** 得分（0-100） */
    private Integer score;

    /** 是否全部答对 */
    private Boolean allCorrect;

    /** 各题作答详情 */
    private List<ScenarioVO.Question> questions;

    /** 总体解析 */
    private String overallExplanation;

    /** 防范要点 */
    private String preventionTips;

    /** 是否记录到学习历史（biz_fraud_detection_log，detect_result=SCENARIO_ANSWER） */
    private Boolean recorded;

    /** 模拟标注 */
    private String simulationNotice;
}
