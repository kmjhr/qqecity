package com.icbc.qingqi.module.safety.service;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 反诈对话演练词库判定器（模拟教学）
 * <p>
 * 双层判定：安全分 = 60 - 危险加权 + 识破加权（0-100 截断）
 * - 命中危险词 -25 / 个，命中识破词 +20 / 个
 * - 无任何命中 → 60 中性；未命中识破词永远达不到 70（不会误判识破）
 * - 危险词：转账、验证码、安全账户、共享屏幕、清空额度、垫资、解冻、保证金、违约金 等
 * - 识破词：96110、110、挂断、核实、报警、官方、不转账、约定口令 等
 * 词库可被剧本 JSON 的 safeWords/dangerWords 扩展（合并去重）
 */
@Component
public class ScenarioWordLibrary {

    /** 危险词 → 教学提示（命中任一即提示） */
    private static final Map<String, String> DANGER_WORDS = new LinkedHashMap<>();

    /** 识破词 */
    private static final List<String> SAFE_WORDS = new ArrayList<>();

    static {
        DANGER_WORDS.put("安全账户", "公检法机关没有「安全账户」，要求转入安全账户验资 100% 是诈骗");
        DANGER_WORDS.put("共享屏幕", "开启屏幕共享等于把手机/账户控制权交给对方");
        DANGER_WORDS.put("屏幕共享", "开启屏幕共享等于把手机/账户控制权交给对方");
        DANGER_WORDS.put("验证码", "银行、公检法绝不会索要短信验证码");
        DANGER_WORDS.put("转账", "涉及转账汇款，务必先通过官方渠道核实对方身份");
        DANGER_WORDS.put("转帐", "涉及转账汇款，务必先通过官方渠道核实对方身份");
        DANGER_WORDS.put("清空额度", "注销/验证账户不需要动用贷款额度");
        DANGER_WORDS.put("贷款额度", "正规流程不会要求你操作贷款额度");
        DANGER_WORDS.put("垫资", "垫资返利是刷单诈骗的典型收割套路");
        DANGER_WORDS.put("解冻", "「卡单/解冻费」是刷单诈骗的续骗话术");
        DANGER_WORDS.put("卡单", "「卡单/解冻费」是刷单诈骗的续骗话术");
        DANGER_WORDS.put("保证金", "先交保证金/押金/手续费的都可能是诈骗");
        DANGER_WORDS.put("违约金", "以违约金威胁是诈骗分子的常见施压话术");
        DANGER_WORDS.put("充值", "「充值返利」是诱饵，小额返利只为引你投入更多");
        DANGER_WORDS.put("收款码", "把收款码发给对方等于送钱上门");
        DANGER_WORDS.put("新账户", "熟人要求转到「新账户/新号码」需高度警惕冒充");
        DANGER_WORDS.put("别告诉", "要求「保密、别告诉任何人」是诈骗分子的控制手段");
        DANGER_WORDS.put("下载app", "陌生 App 可能携带木马或远程控制，勿随意下载");
        DANGER_WORDS.put("下载App", "陌生 App 可能携带木马或远程控制，勿随意下载");

        SAFE_WORDS.add("96110");
        SAFE_WORDS.add("110");
        SAFE_WORDS.add("派出所");
        SAFE_WORDS.add("报警");
        SAFE_WORDS.add("挂断");
        SAFE_WORDS.add("核实");
        SAFE_WORDS.add("官方");
        SAFE_WORDS.add("官方渠道");
        SAFE_WORDS.add("客服电话");
        SAFE_WORDS.add("征信中心");
        SAFE_WORDS.add("不转账");
        SAFE_WORDS.add("不转帐");
        SAFE_WORDS.add("不泄露");
        SAFE_WORDS.add("不垫资");
        SAFE_WORDS.add("约定口令");
        SAFE_WORDS.add("当面");
        SAFE_WORDS.add("原号码");
        SAFE_WORDS.add("家人");
        SAFE_WORDS.add("拉黑");
        SAFE_WORDS.add("视频验证");
        SAFE_WORDS.add("免费");
        SAFE_WORDS.add("骗子");
        SAFE_WORDS.add("诈骗");
        SAFE_WORDS.add("民警");
    }

    /**
     * 判定结果
     */
    @Getter
    public static class Judgement {
        private final int score;
        private final String riskLevel;
        private final List<String> hitSafeWords;
        private final List<String> hitDangerWords;
        private final String dangerHint;

        public Judgement(int score, List<String> hitSafeWords, List<String> hitDangerWords, String dangerHint) {
            this.score = score;
            this.riskLevel = score >= 70 ? "SAFE" : score <= 40 ? "DANGEROUS" : "NEUTRAL";
            this.hitSafeWords = hitSafeWords;
            this.hitDangerWords = hitDangerWords;
            this.dangerHint = dangerHint;
        }
    }

    /**
     * 判定用户话术安全分
     *
     * @param text        用户发言
     * @param extraSafe   剧本扩展识破词（可为空）
     * @param extraDanger 剧本扩展危险词（可为空）
     */
    public Judgement judge(String text, List<String> extraSafe, List<String> extraDanger) {
        String t = text == null ? "" : text.toLowerCase();
        List<String> hitDanger = new ArrayList<>();
        List<String> hitSafe = new ArrayList<>();

        for (Map.Entry<String, String> e : DANGER_WORDS.entrySet()) {
            if (t.contains(e.getKey().toLowerCase())) {
                hitDanger.add(e.getKey());
            }
        }
        if (extraDanger != null) {
            for (String w : extraDanger) {
                if (w != null && !w.isBlank() && t.contains(w.toLowerCase()) && !hitDanger.contains(w)) {
                    hitDanger.add(w);
                }
            }
        }
        for (String w : SAFE_WORDS) {
            if (t.contains(w)) {
                hitSafe.add(w);
            }
        }
        if (extraSafe != null) {
            for (String w : extraSafe) {
                if (w != null && !w.isBlank() && t.contains(w.toLowerCase()) && !hitSafe.contains(w)) {
                    hitSafe.add(w);
                }
            }
        }

        int score = 60 - hitDanger.size() * 25 + hitSafe.size() * 20;
        score = Math.max(0, Math.min(100, score));

        String hint = null;
        if (!hitDanger.isEmpty()) {
            String first = DANGER_WORDS.getOrDefault(hitDanger.get(0),
                    "你提到了「" + hitDanger.get(0) + "」，这是诈骗分子的常见套路");
            hint = "⚠ " + first + "。命中的危险词：" + String.join("、", hitDanger);
        }
        return new Judgement(score, hitSafe, hitDanger, hint);
    }
}
