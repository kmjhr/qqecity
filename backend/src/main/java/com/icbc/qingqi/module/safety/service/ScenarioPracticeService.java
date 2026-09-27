package com.icbc.qingqi.module.safety.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icbc.qingqi.common.BizException;
import com.icbc.qingqi.common.ErrorCode;
import com.icbc.qingqi.module.chat.support.AgentLLMClient;
import com.icbc.qingqi.module.message.entity.SysMessage;
import com.icbc.qingqi.module.message.mapper.SysMessageMapper;
import com.icbc.qingqi.module.safety.dto.*;
import com.icbc.qingqi.module.safety.entity.BizAntiFraudContent;
import com.icbc.qingqi.module.safety.entity.BizScenarioPractice;
import com.icbc.qingqi.module.safety.entity.BizScenarioRound;
import com.icbc.qingqi.module.safety.mapper.BizAntiFraudContentMapper;
import com.icbc.qingqi.module.safety.mapper.BizScenarioPracticeMapper;
import com.icbc.qingqi.module.safety.mapper.BizScenarioRoundMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 反诈对话式演练服务（L3，模拟教学）
 * <p>
 * 玩法：AI（LLM 或本地剧本）扮演诈骗分子施压，用户自由发言，每回合词库判定安全分；
 * 连续 2 回合危险 → LURED；安全分≥70 → SAFE；用户发言≥10 回合 → TIMEOUT；主动结束 → FINISHED。
 * 全程「AI 模拟教学」标识（simulated=true）；结果落站内信。
 * LLM 增强复用 AgentLLMClient（CHAT_LLM_API_KEY 由用户 .env 提供），缺 Key 自动降级本地剧本。
 */
@Slf4j
@Service
public class ScenarioPracticeService {

    /** 用户发言回合上限（超过判 TIMEOUT） */
    private static final int MAX_USER_TURNS = 10;
    /** 安全分 ≥ 70 → 成功识破 */
    private static final int SAFE_THRESHOLD = 70;
    /** 安全分 ≤ 40 → 危险（连续 2 回合被诱骗） */
    private static final int DANGER_THRESHOLD = 40;
    /** 演练中 */
    private static final String RESULT_PLAYING = "PLAYING";

    private final BizScenarioPracticeMapper practiceMapper;
    private final BizScenarioRoundMapper roundMapper;
    private final BizAntiFraudContentMapper contentMapper;
    private final ScenarioWordLibrary wordLibrary;
    private final AgentLLMClient llmClient;
    private final SysMessageMapper messageMapper;
    private final ObjectMapper objectMapper;

    public ScenarioPracticeService(BizScenarioPracticeMapper practiceMapper,
                                   BizScenarioRoundMapper roundMapper,
                                   BizAntiFraudContentMapper contentMapper,
                                   ScenarioWordLibrary wordLibrary,
                                   AgentLLMClient llmClient,
                                   SysMessageMapper messageMapper,
                                   ObjectMapper objectMapper) {
        this.practiceMapper = practiceMapper;
        this.roundMapper = roundMapper;
        this.contentMapper = contentMapper;
        this.wordLibrary = wordLibrary;
        this.llmClient = llmClient;
        this.messageMapper = messageMapper;
        this.objectMapper = objectMapper;
    }

    // ============================================================
    // 1. 开始演练
    // ============================================================

    public PracticeStartVO start(Long userId, Long scenarioId) {
        BizAntiFraudContent c = contentMapper.selectById(scenarioId);
        if (c == null || !"SCENARIO_DIALOG".equals(c.getContentType())) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "情景不存在或不是对话演练情景");
        }
        Script script = parseScript(c.getContent());

        BizScenarioPractice p = new BizScenarioPractice();
        p.setPracticeNo(genPracticeNo());
        p.setUserId(userId);
        p.setScenarioId(scenarioId);
        p.setScenarioTitle(c.getTitle());
        p.setRoundCount(0);
        p.setResult(RESULT_PLAYING);
        p.setRiskScore(0);
        p.setStartedAt(LocalDateTime.now());
        p.setDeleted(0);
        practiceMapper.insert(p);

        // 开场白回合（FRAUD，round_no=1）
        BizScenarioRound opening = new BizScenarioRound();
        opening.setPracticeId(p.getId());
        opening.setRoundNo(1);
        opening.setSpeaker("FRAUD");
        opening.setContent(script.opening);
        roundMapper.insert(opening);
        p.setRoundCount(1);
        practiceMapper.updateById(p);

        PracticeStartVO vo = new PracticeStartVO();
        vo.setPracticeNo(p.getPracticeNo());
        vo.setScenarioTitle(c.getTitle());
        vo.setBackground(script.background);
        vo.setOpeningLine(script.opening);
        vo.setScriptRounds(script.turns.size());
        vo.setTips(List.of(
                "用你自己的话回应 AI 扮演的诈骗分子（可输入：挂断、拨打96110、拒绝转账、核实身份…）",
                "安全分 ≥ 70 判定识破；连续 2 回合低分判定被诱骗；10 回合未定判超时"));
        vo.setSimulated(true);
        return vo;
    }

    // ============================================================
    // 2. 用户发言回合
    // ============================================================

    public PracticeTurnVO turn(Long userId, String practiceNo, String content) {
        BizScenarioPractice p = getOwnedPractice(userId, practiceNo);
        if (!RESULT_PLAYING.equals(p.getResult())) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "演练已结束，请查看回放或重新开始");
        }
        BizAntiFraudContent c = contentMapper.selectById(p.getScenarioId());
        Script script = c != null ? parseScript(c.getContent()) : new Script();

        // 用户回合判定
        ScenarioWordLibrary.Judgement j = wordLibrary.judge(content,
                script.safeWords, script.dangerWords);

        int userTurnNo = (p.getRoundCount() + 1) / 2;
        int newRoundNo = p.getRoundCount() + 1;

        BizScenarioRound userRound = new BizScenarioRound();
        userRound.setPracticeId(p.getId());
        userRound.setRoundNo(newRoundNo);
        userRound.setSpeaker("USER");
        userRound.setContent(content);
        userRound.setSafeScore(j.getScore());
        userRound.setHitWords(String.join("、", j.getHitDangerWords()));
        roundMapper.insert(userRound);
        p.setRoundCount(newRoundNo);
        practiceMapper.updateById(p);

        // 结局判定
        String outcome = null;
        if (isImmediateLure(content)) {
            outcome = "LURED";
        } else if (j.getScore() >= SAFE_THRESHOLD) {
            outcome = "SAFE";
        } else if (j.getScore() <= DANGER_THRESHOLD && lastTwoUserRoundsDangerous(p.getId())) {
            outcome = "LURED";
        } else if (userTurnNo >= MAX_USER_TURNS) {
            outcome = "TIMEOUT";
        }

        PracticeTurnVO vo = new PracticeTurnVO();
        vo.setSafeScore(j.getScore());
        vo.setRiskLevel(j.getRiskLevel());
        vo.setDangerHint(j.getDangerHint());
        vo.setRoundNo(newRoundNo);
        vo.setUserTurnNo(userTurnNo);
        vo.setSimulated(true);

        if (outcome != null) {
            finishPractice(p, outcome, script);
            vo.setGameOver(true);
            vo.setResult(outcome);
            vo.setAiReply(closingLine(outcome));
        } else {
            vo.setGameOver(false);
            vo.setAiReply(nextFraudLine(p, script, userTurnNo, content, c != null ? c.getTitle() : ""));
        }
        return vo;
    }

    // ============================================================
    // 3. 主动结束 / 结算复盘
    // ============================================================

    public PracticeFinishVO finish(Long userId, String practiceNo, String reason) {
        BizScenarioPractice p = getOwnedPractice(userId, practiceNo);
        if (RESULT_PLAYING.equals(p.getResult())) {
            BizAntiFraudContent c = contentMapper.selectById(p.getScenarioId());
            Script script = c != null ? parseScript(c.getContent()) : new Script();
            finishPractice(p, "FINISHED", script);
        }
        return buildFinishVO(p);
    }

    // ============================================================
    // 4. 历史 / 回放
    // ============================================================

    public PracticeHistoryVO history(Long userId, int pageNum, int pageSize) {
        Page<BizScenarioPractice> page = practiceMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<BizScenarioPractice>()
                        .eq(BizScenarioPractice::getUserId, userId)
                        .eq(BizScenarioPractice::getDeleted, 0)
                        .ne(BizScenarioPractice::getResult, RESULT_PLAYING)
                        .orderByDesc(BizScenarioPractice::getCreateTime));
        PracticeHistoryVO vo = new PracticeHistoryVO();
        vo.setTotal(page.getTotal());
        List<PracticeHistoryVO.Item> items = page.getRecords().stream().map(p -> {
            PracticeHistoryVO.Item it = new PracticeHistoryVO.Item();
            it.setPracticeNo(p.getPracticeNo());
            it.setScenarioTitle(p.getScenarioTitle());
            it.setResult(p.getResult());
            it.setResultDesc(p.getResultDesc());
            it.setRiskScore(p.getRiskScore());
            it.setRoundCount(p.getRoundCount());
            it.setStartedAt(p.getStartedAt());
            it.setEndedAt(p.getEndedAt());
            return it;
        }).collect(Collectors.toList());
        vo.setList(items);
        return vo;
    }

    public PracticeDetailVO detail(Long userId, String practiceNo) {
        BizScenarioPractice p = getOwnedPractice(userId, practiceNo);
        List<BizScenarioRound> rounds = roundMapper.selectList(
                new LambdaQueryWrapper<BizScenarioRound>()
                        .eq(BizScenarioRound::getPracticeId, p.getId())
                        .orderByAsc(BizScenarioRound::getRoundNo));
        PracticeDetailVO vo = new PracticeDetailVO();
        vo.setPracticeNo(p.getPracticeNo());
        vo.setScenarioTitle(p.getScenarioTitle());
        vo.setResult(p.getResult());
        vo.setRiskScore(p.getRiskScore());
        vo.setResultDesc(p.getResultDesc());
        vo.setStartedAt(p.getStartedAt());
        vo.setEndedAt(p.getEndedAt());
        List<PracticeDetailVO.RoundItem> items = rounds.stream().map(r -> {
            PracticeDetailVO.RoundItem ri = new PracticeDetailVO.RoundItem();
            ri.setRoundNo(r.getRoundNo());
            ri.setSpeaker(r.getSpeaker());
            ri.setContent(r.getContent());
            ri.setSafeScore(r.getSafeScore());
            ri.setHitWords(r.getHitWords());
            return ri;
        }).collect(Collectors.toList());
        vo.setRounds(items);
        return vo;
    }

    // ============================================================
    // 内部实现
    // ============================================================

    private BizScenarioPractice getOwnedPractice(Long userId, String practiceNo) {
        BizScenarioPractice p = practiceMapper.selectOne(
                new LambdaQueryWrapper<BizScenarioPractice>()
                        .eq(BizScenarioPractice::getPracticeNo, practiceNo)
                        .eq(BizScenarioPractice::getDeleted, 0)
                        .last("LIMIT 1"));
        if (p == null) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "演练不存在");
        }
        if (!p.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权查看/操作他人演练");
        }
        return p;
    }

    /** 最近 2 个用户回合是否都 ≤40（连续危险 → 被诱骗） */
    private boolean lastTwoUserRoundsDangerous(Long practiceId) {
        List<BizScenarioRound> userRounds = roundMapper.selectList(
                new LambdaQueryWrapper<BizScenarioRound>()
                        .eq(BizScenarioRound::getPracticeId, practiceId)
                        .eq(BizScenarioRound::getSpeaker, "USER")
                        .orderByDesc(BizScenarioRound::getRoundNo)
                        .last("LIMIT 2"));
        return userRounds.size() >= 2 && userRounds.stream()
                .allMatch(r -> r.getSafeScore() != null && r.getSafeScore() <= DANGER_THRESHOLD);
    }

    /** 显式危险操作（我要转账 / 发验证码）→ 立即被诱骗 */
    private boolean isImmediateLure(String text) {
        if (text == null) return false;
        boolean explicitTransfer = text.matches(".*(我要转|我转|已转|马上转|这就转|现在转|给你转|转过去|转账成功).*");
        boolean codeGiving = text.matches(".*(验证码|动态码|口令码).*")
                && text.matches(".*(发|给|告诉你|是|填写|输入|报).*");
        return explicitTransfer || codeGiving;
    }

    /** 下一轮诈骗方话术：LLM 增强 → 本地剧本兜底 */
    private String nextFraudLine(BizScenarioPractice p, Script script,
                                 int userTurnNo, String userContent, String title) {
        // LLM 增强（可选，Key 由用户 .env 提供；失败自动降级）
        if (llmClient.isAvailable()) {
            try {
                List<String> history = recentMessages(p.getId());
                String scriptText = script.turns.isEmpty() ? "" :
                        "剧本话术轮次：" + String.join(" | ", script.turns);
                String instruction = "【反诈教学模拟】你正在扮演诈骗分子（情景：" + title + "）。"
                        + scriptText
                        + " 用户刚才说：" + userContent
                        + "。请以诈骗分子身份基于剧本继续施压，话术简短自然（60字内），符合该诈骗类型。";
                String reply = llmClient.chat(script.background, history, instruction);
                if (reply != null && !reply.isBlank()) {
                    return trimReply(reply);
                }
            } catch (Exception e) {
                log.warn("[ScenarioPractice] LLM 增强失败，降级本地剧本：{}", e.getMessage());
            }
        }
        // 本地剧本兜底
        String line;
        if (userTurnNo <= script.turns.size()) {
            line = script.turns.get(userTurnNo - 1);
        } else {
            line = PRESS_LINES[(userTurnNo - script.turns.size() - 1) % PRESS_LINES.length];
        }
        // 存 FRAUD 回合
        saveFraudRound(p, line);
        return line;
    }

    private static final String[] PRESS_LINES = {
            "你真的不再考虑一下？错过这单损失的是你。",
            "行吧，我给你最后两分钟考虑，过了这村没这店。",
            "你不处理的话，后果自负，别怪我没提醒你。",
            "你再犹豫，账户就要被冻结了，抓紧时间。",
            "我已经尽力帮你了，你这样做只会害了你自己。"
    };

    private void saveFraudRound(BizScenarioPractice p, String line) {
        BizScenarioRound r = new BizScenarioRound();
        r.setPracticeId(p.getId());
        r.setRoundNo(p.getRoundCount() + 1);
        r.setSpeaker("FRAUD");
        r.setContent(line);
        roundMapper.insert(r);
        p.setRoundCount(p.getRoundCount() + 1);
        practiceMapper.updateById(p);
    }

    private List<String> recentMessages(Long practiceId) {
        List<BizScenarioRound> rounds = roundMapper.selectList(
                new LambdaQueryWrapper<BizScenarioRound>()
                        .eq(BizScenarioRound::getPracticeId, practiceId)
                        .orderByDesc(BizScenarioRound::getRoundNo)
                        .last("LIMIT 4"));
        List<String> history = new ArrayList<>();
        for (int i = rounds.size() - 1; i >= 0; i--) {
            BizScenarioRound r = rounds.get(i);
            history.add("USER".equals(r.getSpeaker()) ? r.getContent() : "【诈骗分子】" + r.getContent());
        }
        return history;
    }

    private String trimReply(String reply) {
        reply = reply.replaceAll("^[\"“”'‘’\\s]+|[\"“”'‘’\\s]+$", "");
        return reply.length() > 120 ? reply.substring(0, 120) : reply;
    }

    /** 结算：写结果/风险分/复盘、发站内信 */
    private void finishPractice(BizScenarioPractice p, String outcome, Script script) {
        // 综合风险分 = 100 - 用户回合平均安全分（越低越安全）
        List<BizScenarioRound> userRounds = roundMapper.selectList(
                new LambdaQueryWrapper<BizScenarioRound>()
                        .eq(BizScenarioRound::getPracticeId, p.getId())
                        .eq(BizScenarioRound::getSpeaker, "USER"));
        int avg = userRounds.isEmpty() ? 60
                : (int) Math.round(userRounds.stream().mapToInt(BizScenarioRound::getSafeScore).average().orElse(60));
        int riskScore = 100 - avg;
        if ("SAFE".equals(outcome)) riskScore = Math.min(riskScore, 30);
        if ("LURED".equals(outcome)) riskScore = Math.max(riskScore, 60);
        riskScore = Math.max(0, Math.min(100, riskScore));

        String desc = buildResultDesc(outcome, userRounds, script);
        p.setResult(outcome);
        p.setRiskScore(riskScore);
        p.setResultDesc(desc);
        p.setEndedAt(LocalDateTime.now());
        practiceMapper.updateById(p);

        // 站内信（模拟教学复盘）
        SysMessage msg = new SysMessage();
        msg.setUserId(p.getUserId());
        msg.setTitle("反诈演练复盘：" + resultName(outcome));
        msg.setContent("【AI 模拟教学】" + p.getScenarioTitle() + "｜结果：" + resultName(outcome)
                + "｜风险分：" + riskScore + "/100\n" + desc);
        msg.setType("BUSINESS");
        msg.setBizType("ANTI_FRAUD");
        msg.setBizId(p.getId());
        msg.setIsRead(0);
        messageMapper.insert(msg);
    }

    private String buildResultDesc(String outcome, List<BizScenarioRound> userRounds, Script script) {
        List<String> dangerWords = userRounds.stream()
                .map(BizScenarioRound::getHitWords)
                .filter(w -> w != null && !w.isBlank())
                .distinct()
                .collect(Collectors.toList());
        switch (outcome) {
            case "SAFE":
                return "恭喜，你成功识破了骗局！演练中你的应对体现了警觉：不轻信、核实身份、拒绝转账。"
                        + "继续保持「先核实、后行动」的习惯，任何要求转账/验证码的都是诈骗。";
            case "LURED":
                return "本次演练中你被诈骗话术带偏了。骗子利用" + (dangerWords.isEmpty() ? "施压与诱导" : "「" + String.join("、", dangerWords) + "」")
                        + "话术让你逐步放松警惕。正确做法：立即挂断，拨打 96110 / 110 核实，绝不转账。";
            case "TIMEOUT":
                return "10 回合未定胜负，说明你有所警觉但不够果断。识别到危险信号（转账、验证码、安全账户）应立刻挂断核实，而不是继续对话。";
            default:
                return "你主动结束了演练。复盘要点：涉及资金的操作一律先通过官方渠道核实，必要时报警。";
        }
    }

    private String closingLine(String outcome) {
        switch (outcome) {
            case "SAFE":
                return "（诈骗分子）……你居然识破了？算你厉害。记住：以后遇到这种事，先挂断、再核实！";
            case "LURED":
                return "（诈骗分子）哈哈，其实我是骗子！你刚才已经掉进我的陷阱了。别怕，这是模拟教学，请记住这个套路。";
            case "TIMEOUT":
                return "（诈骗分子）算了，磨这么久也没结果，收工。（模拟教学：你坚持到了最后，但始终未明确识破）";
            default:
                return "";
        }
    }

    private PracticeFinishVO buildFinishVO(BizScenarioPractice p) {
        PracticeFinishVO vo = new PracticeFinishVO();
        vo.setResult(p.getResult());
        vo.setResultName(resultName(p.getResult()));
        vo.setRiskScore(p.getRiskScore());
        vo.setWarningLevel(p.getRiskScore() <= 30 ? "LOW"
                : p.getRiskScore() <= 50 ? "MEDIUM"
                : p.getRiskScore() <= 70 ? "HIGH" : "CRITICAL");
        vo.setResultDesc(p.getResultDesc());
        List<String> points = new ArrayList<>();
        points.add(resultName(p.getResult()) + "｜风险分 " + p.getRiskScore() + "/100");
        if ("SAFE".equals(p.getResult())) {
            points.add("识破要点：不轻信、先核实、拒绝转账——你做对了");
        } else if ("LURED".equals(p.getResult())) {
            points.add("被骗原因：被「转账/验证码/安全账户」类话术引导，未及时挂断核实");
            points.add("正确做法：立即挂断，拨打 96110 / 110 核实，绝不向任何账户转账");
        } else {
            points.add("复盘要点：涉及资金的操作一律先通过官方渠道核实，必要时报警");
        }
        points.add("通用提示：公检法/客服不会要求转账；征信异议处理免费无转账；熟人借钱先换原号核实");
        vo.setReviewPoints(points);
        vo.setSimulated(true);
        return vo;
    }

    private String resultName(String result) {
        switch (result) {
            case "SAFE": return "成功识破";
            case "LURED": return "被诱骗";
            case "TIMEOUT": return "超时未定";
            case "FINISHED": return "主动结束";
            default: return "演练中";
        }
    }

    // ============================================================
    // 剧本解析
    // ============================================================

    private Script parseScript(String content) {
        Script s = new Script();
        if (content == null || content.isBlank()) return s;
        try {
            JsonNode root = objectMapper.readTree(content);
            s.background = textOr(root, "background", "");
            s.opening = textOr(root, "opening", "（开场白缺失）");
            JsonNode turns = root.get("turns");
            if (turns != null && turns.isArray()) {
                for (JsonNode t : turns) {
                    JsonNode f = t.get("fraud");
                    if (f != null && !f.asText().isBlank()) {
                        s.turns.add(f.asText());
                    }
                }
            }
            JsonNode safe = root.get("safeWords");
            if (safe != null && safe.isArray()) safe.forEach(n -> s.safeWords.add(n.asText()));
            JsonNode danger = root.get("dangerWords");
            if (danger != null && danger.isArray()) danger.forEach(n -> s.dangerWords.add(n.asText()));
        } catch (Exception e) {
            log.warn("[ScenarioPractice] 剧本解析失败，使用空剧本：{}", e.getMessage());
        }
        return s;
    }

    private String textOr(JsonNode root, String field, String def) {
        JsonNode n = root.get(field);
        return n != null && !n.asText().isBlank() ? n.asText() : def;
    }

    private String genPracticeNo() {
        return "PRAC" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
    }

    /** 剧本模型 */
    private static class Script {
        private String background = "";
        private String opening = "";
        private List<String> turns = new ArrayList<>();
        private List<String> safeWords = new ArrayList<>();
        private List<String> dangerWords = new ArrayList<>();
    }
}
