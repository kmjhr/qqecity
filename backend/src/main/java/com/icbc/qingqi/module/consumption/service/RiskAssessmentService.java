package com.icbc.qingqi.module.consumption.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icbc.qingqi.common.BizException;
import com.icbc.qingqi.common.ErrorCode;
import com.icbc.qingqi.module.consumption.dto.RiskAssessmentSubmitDTO;
import com.icbc.qingqi.module.consumption.dto.RiskAssessmentVO;
import com.icbc.qingqi.module.consumption.dto.RiskQuestionVO;
import com.icbc.qingqi.module.consumption.entity.BizRiskAssessment;
import com.icbc.qingqi.module.consumption.mapper.BizRiskAssessmentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 风险测评服务
 * <p>
 * 缺口 #7 理财匹配与风险测评（前置）
 * - 10 题问卷，每题 1-4 分，总分 10-40
 * - 10-18 → CONSERVATIVE 保守型
 * - 19-30 → STEADY 稳健型
 * - 31-40 → BALANCED 平衡型
 * - 有效期 1 年，过期需重新测评
 * - 首次购买强制测评，未测评不能进推荐
 */
@Slf4j
@Service
public class RiskAssessmentService {

    private static final int TOTAL_QUESTIONS = 10;
    private static final int VALID_YEARS = 1;
    private static final int CONSERVATIVE_MAX = 18;
    private static final int STEADY_MAX = 30;

    private final BizRiskAssessmentMapper assessmentMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RiskAssessmentService(BizRiskAssessmentMapper assessmentMapper) {
        this.assessmentMapper = assessmentMapper;
    }

    /**
     * 获取风险测评问卷（10 题）
     */
    public List<RiskQuestionVO> getQuestionnaire() {
        return QuestionnaireBuilder.build();
    }

    /**
     * 提交测评答案，计算得分并落库
     */
    @Transactional(rollbackFor = Exception.class)
    public RiskAssessmentVO submit(Long userId, RiskAssessmentSubmitDTO dto) {
        Map<Integer, String> answers = dto.getAnswers();
        if (answers.size() != TOTAL_QUESTIONS) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "测评答案不完整，应答 " + TOTAL_QUESTIONS + " 题，实际 " + answers.size() + " 题");
        }

        List<RiskQuestionVO> questions = getQuestionnaire();
        int totalScore = 0;
        List<RiskAssessmentVO.AnswerDetail> details = new ArrayList<>();
        List<Map<String, Object>> answerJson = new ArrayList<>();

        for (RiskQuestionVO q : questions) {
            String optCode = answers.get(q.getId());
            if (optCode == null || optCode.isEmpty()) {
                throw new BizException(ErrorCode.PARAM_ERROR, "第 " + q.getId() + " 题未作答");
            }
            RiskQuestionVO.Option opt = q.getOptions().stream()
                    .filter(o -> o.getCode().equals(optCode))
                    .findFirst()
                    .orElseThrow(() -> new BizException(ErrorCode.PARAM_ERROR,
                            "第 " + q.getId() + " 题选项 " + optCode + " 无效"));

            totalScore += opt.getScore();

            RiskAssessmentVO.AnswerDetail detail = new RiskAssessmentVO.AnswerDetail();
            detail.setQuestionId(q.getId());
            detail.setOptionCode(optCode);
            detail.setScore(opt.getScore());
            detail.setQuestionTitle(q.getTitle());
            detail.setOptionLabel(opt.getLabel());
            details.add(detail);

            Map<String, Object> a = new LinkedHashMap<>();
            a.put("q", q.getId());
            a.put("opt", optCode);
            a.put("score", opt.getScore());
            answerJson.add(a);
        }

        String riskLevel = mapScoreToLevel(totalScore);
        String riskLevelName = levelName(riskLevel);
        String riskLevelDesc = levelDesc(riskLevel);

        // 旧记录置为非最新
        assessmentMapper.update(null,
                new LambdaUpdateWrapper<BizRiskAssessment>()
                        .eq(BizRiskAssessment::getUserId, userId)
                        .eq(BizRiskAssessment::getIsLatest, 1)
                        .set(BizRiskAssessment::getIsLatest, 0));

        // 写入新记录
        BizRiskAssessment record = new BizRiskAssessment();
        record.setUserId(userId);
        record.setAssessNo("RA" + System.currentTimeMillis());
        try {
            record.setAnswers(objectMapper.writeValueAsString(answerJson));
        } catch (Exception e) {
            record.setAnswers("[]");
        }
        record.setTotalScore(totalScore);
        record.setRiskLevel(riskLevel);
        record.setRiskLevelName(riskLevelName);
        record.setValidUntil(LocalDate.now().plusYears(VALID_YEARS));
        record.setIsLatest(1);
        record.setAssessTime(LocalDateTime.now());
        assessmentMapper.insert(record);

        log.info("[风险测评] 用户={}, 总分={}, 等级={}, 有效期至={}",
                userId, totalScore, riskLevel, record.getValidUntil());

        return toVO(record, details, riskLevelDesc);
    }

    /**
     * 查询当前用户最新测评结果
     */
    public RiskAssessmentVO latest(Long userId) {
        BizRiskAssessment record = assessmentMapper.selectOne(
                new LambdaQueryWrapper<BizRiskAssessment>()
                        .eq(BizRiskAssessment::getUserId, userId)
                        .eq(BizRiskAssessment::getIsLatest, 1)
                        .last("LIMIT 1"));
        if (record == null) {
            return null;
        }
        return toVO(record, null, levelDesc(record.getRiskLevel()));
    }

    /**
     * 校验用户是否已完成有效测评
     *
     * @return 未完成或已过期返回 false
     */
    public boolean hasValidAssessment(Long userId) {
        RiskAssessmentVO vo = latest(userId);
        if (vo == null) return false;
        return !Boolean.TRUE.equals(vo.getExpired());
    }

    /**
     * 查询历史测评记录
     */
    public List<RiskAssessmentVO> history(Long userId) {
        List<BizRiskAssessment> records = assessmentMapper.selectList(
                new LambdaQueryWrapper<BizRiskAssessment>()
                        .eq(BizRiskAssessment::getUserId, userId)
                        .orderByDesc(BizRiskAssessment::getAssessTime));
        List<RiskAssessmentVO> list = new ArrayList<>();
        for (BizRiskAssessment r : records) {
            list.add(toVO(r, null, levelDesc(r.getRiskLevel())));
        }
        return list;
    }

    // ============================================================
    //  工具方法
    // ============================================================

    private String mapScoreToLevel(int score) {
        if (score <= CONSERVATIVE_MAX) return "CONSERVATIVE";
        if (score <= STEADY_MAX) return "STEADY";
        return "BALANCED";
    }

    private String levelName(String level) {
        return switch (level) {
            case "CONSERVATIVE" -> "保守型";
            case "STEADY" -> "稳健型";
            case "BALANCED" -> "平衡型";
            default -> level;
        };
    }

    private String levelDesc(String level) {
        return switch (level) {
            case "CONSERVATIVE" -> "保守型：本金安全优先，不希望承担任何本金损失风险。仅适合配置 R1 低风险产品。";
            case "STEADY" -> "稳健型：可承受小幅波动，追求稳定收益。适合 R1-R2 低/中低风险产品。";
            case "BALANCED" -> "平衡型：可承受一定波动，追求中长期增值。适合 R1-R2 产品，可适当配置 R2 中低风险。";
            default -> level;
        };
    }

    private RiskAssessmentVO toVO(BizRiskAssessment r, List<RiskAssessmentVO.AnswerDetail> details, String desc) {
        RiskAssessmentVO vo = new RiskAssessmentVO();
        vo.setId(r.getId());
        vo.setUserId(r.getUserId());
        vo.setAssessNo(r.getAssessNo());
        vo.setTotalScore(r.getTotalScore());
        vo.setRiskLevel(r.getRiskLevel());
        vo.setRiskLevelName(r.getRiskLevelName());
        vo.setRiskLevelDesc(desc);
        vo.setValidUntil(r.getValidUntil());
        vo.setExpired(r.getValidUntil() != null && r.getValidUntil().isBefore(LocalDate.now()));
        vo.setIsLatest(r.getIsLatest());
        vo.setAssessTime(r.getAssessTime());
        vo.setAnswerDetails(details != null ? details : List.of());
        return vo;
    }

    // ============================================================
    //  问卷题库（硬编码 10 题）
    // ============================================================

    private static class QuestionnaireBuilder {
        static List<RiskQuestionVO> build() {
            List<RiskQuestionVO> list = new ArrayList<>();
            list.add(q(1, "您的年龄范围？",
                    o("A", "18-30 岁", 4), o("B", "31-45 岁", 3), o("C", "46-55 岁", 2), o("D", "55 岁以上", 1)));
            list.add(q(2, "您的家庭年收入（含工资/经营/投资）？",
                    o("A", "100 万以上", 4), o("B", "50-100 万", 3), o("C", "20-50 万", 2), o("D", "20 万以下", 1)));
            list.add(q(3, "您计划用于理财的资金占家庭金融资产的比例？",
                    o("A", "60% 以上", 4), o("B", "30%-60%", 3), o("C", "10%-30%", 2), o("D", "10% 以下", 1)));
            list.add(q(4, "您的投资期限预期？",
                    o("A", "5 年以上", 4), o("B", "3-5 年", 3), o("C", "1-3 年", 2), o("D", "1 年以内", 1)));
            list.add(q(5, "您能接受的本金最大亏损比例？",
                    o("A", "20% 以上", 4), o("B", "10%-20%", 3), o("C", "5%-10%", 2), o("D", "不能接受任何亏损", 1)));
            list.add(q(6, "您购买理财/基金/股票的经验？",
                    o("A", "5 年以上，多品类", 4), o("B", "3-5 年，2-3 品类", 3), o("C", "1-3 年，1-2 品类", 2), o("D", "无经验", 1)));
            list.add(q(7, "您对金融产品的了解程度？",
                    o("A", "精通，能独立分析", 4), o("B", "比较了解", 3), o("C", "略知一二", 2), o("D", "完全不了解", 1)));
            list.add(q(8, "您的主要收入来源稳定性？",
                    o("A", "经营收入，波动较大", 4), o("B", "工资收入，稳定增长", 3), o("C", "工资收入，较稳定", 2), o("D", "退休金/利息，固定不变", 1)));
            list.add(q(9, "您家庭是否有应急资金（≥6 个月支出）？",
                    o("A", "无，需要用投资应急", 4), o("B", "有，但不足 6 个月", 3), o("C", "有，刚好 6 个月", 2), o("D", "有，超过 6 个月", 1)));
            list.add(q(10, "如果您的投资在 1 个月内亏损 10%，您会？",
                    o("A", "加仓摊薄成本", 4), o("B", "继续持有不动", 3), o("C", "部分赎回止损", 2), o("D", "立即全部赎回", 1)));
            return list;
        }

        private static RiskQuestionVO q(int id, String title, RiskQuestionVO.Option... opts) {
            RiskQuestionVO vo = new RiskQuestionVO();
            vo.setId(id);
            vo.setTitle(title);
            vo.setOptions(Arrays.asList(opts));
            return vo;
        }

        private static RiskQuestionVO.Option o(String code, String label, int score) {
            RiskQuestionVO.Option opt = new RiskQuestionVO.Option();
            opt.setCode(code);
            opt.setLabel(label);
            opt.setScore(score);
            return opt;
        }
    }
}
