package com.icbc.qingqi.module.safety.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icbc.qingqi.common.BizException;
import com.icbc.qingqi.common.ErrorCode;
import com.icbc.qingqi.module.safety.dto.ScenarioSubmitDTO;
import com.icbc.qingqi.module.safety.dto.ScenarioSubmitVO;
import com.icbc.qingqi.module.safety.dto.ScenarioVO;
import com.icbc.qingqi.module.safety.entity.BizAntiFraudContent;
import com.icbc.qingqi.module.safety.entity.BizFraudDetectionLog;
import com.icbc.qingqi.module.safety.mapper.BizAntiFraudContentMapper;
import com.icbc.qingqi.module.safety.mapper.BizFraudDetectionLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 反诈情景化教学服务（模拟）
 * <p>
 * 缺口 #1 反诈情景化教学
 * 基于 biz_anti_fraud_content 扩展 content_type=SCENARIO_SIM
 * 互动问答：刷单诈骗/冒充公检法/征信洗白
 * 选择 → 反馈 → 解析；选错有纠偏文案
 * 进度可续：作答记录持久化到 biz_fraud_detection_log（detect_result=SCENARIO_ANSWER）
 */
@Slf4j
@Service
public class ScenarioTeachingService {

    /** 情景模拟类型标识 */
    private static final String CONTENT_TYPE_SCENARIO = "SCENARIO_SIM";

    private final BizAntiFraudContentMapper contentMapper;
    private final BizFraudDetectionLogMapper logMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ScenarioTeachingService(BizAntiFraudContentMapper contentMapper,
                                   BizFraudDetectionLogMapper logMapper) {
        this.contentMapper = contentMapper;
        this.logMapper = logMapper;
    }

    /**
     * 列出全部情景模拟内容
     */
    public List<BizAntiFraudContent> listScenarios(String category) {
        LambdaQueryWrapper<BizAntiFraudContent> wrapper = new LambdaQueryWrapper<BizAntiFraudContent>()
                .eq(BizAntiFraudContent::getContentType, CONTENT_TYPE_SCENARIO)
                .eq(BizAntiFraudContent::getStatus, 1)
                .orderByAsc(BizAntiFraudContent::getSortOrder);
        if (category != null && !category.isEmpty()) {
            wrapper.eq(BizAntiFraudContent::getCategory, category);
        }
        return contentMapper.selectList(wrapper);
    }

    /**
     * 获取情景详情（含问题与选项；不暴露正确答案）
     */
    public ScenarioVO getScenario(Long scenarioId) {
        BizAntiFraudContent content = contentMapper.selectById(scenarioId);
        if (content == null || !CONTENT_TYPE_SCENARIO.equals(content.getContentType())) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "情景模拟内容不存在");
        }
        ScenarioVO vo = parseContent(content);
        // 列表/详情时移除答案字段
        for (ScenarioVO.Question q : vo.getQuestions()) {
            q.setExplanation(null);
            q.setUserChoice(null);
            q.setCorrect(null);
            q.setCorrection(null);
        }
        // 浏览量 +1
        content.setViewCount((content.getViewCount() == null ? 0 : content.getViewCount()) + 1);
        contentMapper.updateById(content);
        return vo;
    }

    /**
     * 提交答案：评分、纠偏、记录到 biz_fraud_detection_log
     */
    @Transactional(rollbackFor = Exception.class)
    public ScenarioSubmitVO submit(Long userId, Long scenarioId, ScenarioSubmitDTO dto) {
        BizAntiFraudContent content = contentMapper.selectById(scenarioId);
        if (content == null || !CONTENT_TYPE_SCENARIO.equals(content.getContentType())) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "情景模拟内容不存在");
        }
        ScenarioVO vo = parseContent(content);
        Map<Integer, String> correctAnswers = extractCorrectAnswers(vo);
        Map<Integer, String> userAnswers = dto.getAnswers() == null ? Map.of() : dto.getAnswers();

        int total = vo.getQuestions().size();
        int correctCount = 0;
        List<ScenarioVO.Question> resultQuestions = new ArrayList<>();

        for (ScenarioVO.Question q : vo.getQuestions()) {
            ScenarioVO.Question rq = new ScenarioVO.Question();
            rq.setQuestionNo(q.getQuestionNo());
            rq.setStem(q.getStem());
            rq.setOptions(q.getOptions());

            String userChoice = userAnswers.get(q.getQuestionNo());
            String correctChoice = correctAnswers.get(q.getQuestionNo());

            rq.setUserChoice(userChoice == null ? "未作答" : userChoice);
            boolean isCorrect = userChoice != null && userChoice.equals(correctChoice);
            rq.setCorrect(isCorrect);
            if (isCorrect) {
                correctCount++;
            } else {
                rq.setCorrection(buildCorrection(q.getQuestionNo(), userChoice, correctChoice, content.getCategory()));
            }
            rq.setExplanation(q.getExplanation());
            resultQuestions.add(rq);
        }

        int accuracy = total == 0 ? 0 : (correctCount * 100 / total);
        boolean allCorrect = correctCount == total;

        ScenarioSubmitVO submitVO = new ScenarioSubmitVO();
        submitVO.setScenarioId(scenarioId);
        submitVO.setTotalQuestions(total);
        submitVO.setCorrectCount(correctCount);
        submitVO.setAccuracy(accuracy);
        submitVO.setScore(accuracy);
        submitVO.setAllCorrect(allCorrect);
        submitVO.setQuestions(resultQuestions);
        submitVO.setOverallExplanation(buildOverallExplanation(content.getCategory(), allCorrect));
        submitVO.setPreventionTips(buildPreventionTips(content.getCategory()));
        submitVO.setSimulationNotice("情景教学为模拟内容，仅用于提升反诈意识，不构成法律/金融建议。");

        // 持久化到 biz_fraud_detection_log（复用表，避免新增表，符合 §1.3 原则）
        BizFraudDetectionLog detectLog = new BizFraudDetectionLog();
        detectLog.setUserId(userId);
        detectLog.setInputText("情景模拟[" + content.getCategory() + "] 答对 " + correctCount + "/" + total
                + " 用户选择：" + userAnswers);
        detectLog.setDetectResult("SCENARIO_ANSWER");
        detectLog.setRiskLevel(allCorrect ? 1 : (correctCount >= total / 2 ? 3 : 5));
        detectLog.setMatchedRules(objectToJsonString(userAnswers));
        detectLog.setWarningContent(submitVO.getOverallExplanation() + "\n" + submitVO.getPreventionTips());
        detectLog.setDetectTime(LocalDateTime.now());
        logMapper.insert(detectLog);
        submitVO.setRecorded(true);

        log.info("[情景教学] 用户={}, scenarioId={}, {}/{} 答对", userId, scenarioId, correctCount, total);
        return submitVO;
    }

    // ============================================================
    //  内部方法
    // ============================================================

    /**
     * 解析 content 字段（JSON）→ ScenarioVO
     * <p>
     * content 字段格式：
     * {
     *   "background": "情景背景",
     *   "questions": [
     *     {
     *       "questionNo": 1,
     *       "stem": "题干",
     *       "options": [{"key":"A","text":"..."}, ...],
     *       "correctAnswer": "B",
     *       "explanation": "解析"
     *     }
     *   ]
     * }
     */
    @SuppressWarnings("unchecked")
    private ScenarioVO parseContent(BizAntiFraudContent content) {
        ScenarioVO vo = new ScenarioVO();
        vo.setId(content.getId());
        vo.setTitle(content.getTitle());
        vo.setCategory(content.getCategory());
        vo.setSummary(content.getSummary());
        vo.setSimulationNotice("情景教学为模拟内容，仅用于提升反诈意识。");

        try {
            Map<String, Object> data = objectMapper.readValue(content.getContent(), Map.class);
            vo.setBackground((String) data.get("background"));
            List<Map<String, Object>> rawQuestions = (List<Map<String, Object>>) data.get("questions");
            if (rawQuestions == null) {
                vo.setQuestions(List.of());
                return vo;
            }
            List<ScenarioVO.Question> questions = new ArrayList<>();
            for (Map<String, Object> rq : rawQuestions) {
                ScenarioVO.Question q = new ScenarioVO.Question();
                q.setQuestionNo((Integer) rq.get("questionNo"));
                q.setStem((String) rq.get("stem"));
                q.setExplanation((String) rq.get("explanation"));
                // 选项中正确标记需要移除（仅后端校验用，通过 correctAnswer 字段标识）
                List<Map<String, Object>> rawOpts = (List<Map<String, Object>>) rq.get("options");
                List<ScenarioVO.Option> opts = new ArrayList<>();
                if (rawOpts != null) {
                    for (Map<String, Object> ro : rawOpts) {
                        ScenarioVO.Option o = new ScenarioVO.Option();
                        o.setKey((String) ro.get("key"));
                        o.setText((String) ro.get("text"));
                        opts.add(o);
                    }
                }
                q.setOptions(opts);
                questions.add(q);
            }
            vo.setQuestions(questions);
        } catch (Exception e) {
            log.error("[情景教学] 内容解析失败：scenarioId={}, err={}", content.getId(), e.getMessage());
            vo.setQuestions(List.of());
            vo.setBackground("（情景内容格式异常，请联系管理员）");
        }
        return vo;
    }

    /**
     * 从原始 content 中提取正确答案（题号 → 正确选项 key）
     */
    @SuppressWarnings("unchecked")
    private Map<Integer, String> extractCorrectAnswers(ScenarioVO vo) {
        // 重新读取原始 content 拿到 correctAnswer 字段
        BizAntiFraudContent content = contentMapper.selectById(vo.getId());
        Map<Integer, String> answers = new HashMap<>();
        try {
            Map<String, Object> data = objectMapper.readValue(content.getContent(), Map.class);
            List<Map<String, Object>> rawQuestions = (List<Map<String, Object>>) data.get("questions");
            if (rawQuestions != null) {
                for (Map<String, Object> rq : rawQuestions) {
                    Integer questionNo = (Integer) rq.get("questionNo");
                    String correctAnswer = (String) rq.get("correctAnswer");
                    if (questionNo != null && correctAnswer != null) {
                        answers.put(questionNo, correctAnswer);
                    }
                }
            }
        } catch (Exception e) {
            log.error("[情景教学] 提取正确答案失败：scenarioId={}", vo.getId(), e);
        }
        return answers;
    }

    private String buildCorrection(Integer questionNo, String userChoice,
                                    String correctChoice, String category) {
        StringBuilder sb = new StringBuilder();
        sb.append("【纠偏·选错】");
        if (userChoice == null || "未作答".equals(userChoice)) {
            sb.append("本题未作答。");
        } else {
            sb.append("你选择了 ").append(userChoice).append("，正确答案为 ").append(correctChoice).append("。");
        }
        sb.append(specificCorrection(category, questionNo));
        return sb.toString();
    }

    private String specificCorrection(String category, Integer questionNo) {
        if ("刷单诈骗".equals(category)) {
            switch (questionNo) {
                case 1: return "所有要求先交押金的刷单兼职都是诈骗，刷单本身也是违法行为，切勿参与。";
                case 2: return "「日赚数百、零门槛」是典型刷单话术，正经兼职不会承诺如此高薪；任何高额回报都伴随高风险或诈骗。";
                case 3: return "一旦发现是诈骗应立即停止操作、保留证据并报警，继续转账只会扩大损失。";
                default: return "刷单诈骗的核心特征是「先付后返」「小额返利引诱大额投入」，识别后应立即止损。";
            }
        }
        if ("冒充公检法".equals(category)) {
            switch (questionNo) {
                case 1: return "公检法机关绝无「安全账户」概念，也不会通过电话/微信办案；要求转账到「安全账户」100% 是诈骗。";
                case 2: return "正规办案流程不会通过 QQ/微信发送「拘捕令」；任何要求点击陌生链接的都是钓鱼。";
                case 3: return "遇到疑似公检法诈骗，应立即挂断电话并拨打 110 核实，绝不在对方指示下转账。";
                default: return "冒充公检法的核心话术：「涉嫌洗钱」「安全账户」「资金核查」，遇此立即挂断报警。";
            }
        }
        if ("征信洗白".equals(category)) {
            switch (questionNo) {
                case 1: return "征信领域不存在「修复」「洗白」「铲单」概念，任何声称可花钱消除不良记录的都是诈骗。";
                case 2: return "正规异议申请不收费，可通过人民银行征信中心办理，处理结果由金融机构根据事实判定。";
                case 3: return "伪造材料「修复」征信属违法行为，可能构成伪造印章罪、诈骗罪，切勿轻信。";
                default: return "征信洗白的核心话术：「内部渠道」「特殊关系」「花钱消记录」，遇此立即拒绝并报警。";
            }
        }
        return "请重新阅读题目解析并加强防范意识。";
    }

    private String buildOverallExplanation(String category, boolean allCorrect) {
        StringBuilder sb = new StringBuilder();
        sb.append("【").append(category).append("·情景教学总评】\n");
        if (allCorrect) {
            sb.append("恭喜全部答对！你已经具备识别此类诈骗的核心能力。");
        } else {
            sb.append("仍有题目答错，请重点阅读下方「防范要点」并复习错题解析。");
        }
        sb.append("\n（情景教学为模拟内容，仅供提升反诈意识）");
        return sb.toString();
    }

    private String buildPreventionTips(String category) {
        switch (category) {
            case "刷单诈骗":
                return "【防范要点】\n1. 不轻信「先付后返」「高薪兼职」话术；\n2. 不下载陌生人指定的APP；\n3. 任何要求垫付资金的兼职都是诈骗；\n4. 一旦被骗立即保留聊天记录、转账凭证并拨打 110。";
            case "冒充公检法":
                return "【防范要点】\n1. 公检法不会通过电话/微信办案，更无「安全账户」；\n2. 不点击陌生链接、不下载来源不明的「办案软件」；\n3. 接到「涉嫌洗钱」电话立即挂断并拨打 110 核实；\n4. 不向陌生账户转账、不告知短信验证码。";
            case "征信洗白":
                return "【防范要点】\n1. 征信领域无「修复/洗白/铲单」概念，凡收费消除记录都是诈骗；\n2. 真正异议申请请到人民银行征信中心办理，且不收费；\n3. 不向陌生人提供身份证、银行卡、手机号；\n4. 发现被骗立即报警并联系银行挂失账户。";
            default:
                return "【防范要点】\n未知链接不点击、陌生来电不轻信、个人信息不透露、转账汇款多核实。";
        }
    }

    private String objectToJsonString(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }
}
