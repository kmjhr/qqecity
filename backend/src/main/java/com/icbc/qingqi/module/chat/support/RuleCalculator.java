package com.icbc.qingqi.module.chat.support;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 规则推理计算（模拟）：从用户消息识别"押金/保函费 + 金额"，按知识库费率档位计算保函费
 * <p>
 * 演示用：费率口径与 FaqData 保函费率一致（≤2000→0.8%，≤4000→1.0%，≤6000→1.2%，>6000→1.5%）
 */
@Component
public class RuleCalculator {

    private static final BigDecimal LIMIT_1 = BigDecimal.valueOf(2000);
    private static final BigDecimal LIMIT_2 = BigDecimal.valueOf(4000);
    private static final BigDecimal LIMIT_3 = BigDecimal.valueOf(6000);
    private static final BigDecimal RATE_1 = new BigDecimal("0.008");
    private static final BigDecimal RATE_2 = new BigDecimal("0.010");
    private static final BigDecimal RATE_3 = new BigDecimal("0.012");
    private static final BigDecimal RATE_4 = new BigDecimal("0.015");

    private static final Pattern AMOUNT = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(万|元|块)?");

    /**
     * 若消息涉及保函/押金费用且含金额，返回"算给你看"的说明；否则返回 null
     */
    public String guaranteeFee(String message) {
        if (message == null || message.isBlank()) return null;
        boolean about = message.contains("押金") || message.contains("保函费")
                || message.contains("保函") || message.contains("租金")
                || (message.contains("费") && message.contains("多少"));
        if (!about) return null;

        // 提取消息中的金额（取最后一个数字，演示口径）
        Matcher m = AMOUNT.matcher(message);
        BigDecimal amount = null;
        while (m.find()) {
            BigDecimal v = new BigDecimal(m.group(1));
            String unit = m.group(2);
            if ("万".equals(unit)) v = v.multiply(BigDecimal.valueOf(10000));
            amount = v;
        }
        if (amount == null) return null;

        BigDecimal rate;
        String tier;
        if (amount.compareTo(LIMIT_1) <= 0) {
            rate = RATE_1; tier = "≤2000";
        } else if (amount.compareTo(LIMIT_2) <= 0) {
            rate = RATE_2; tier = "≤4000";
        } else if (amount.compareTo(LIMIT_3) <= 0) {
            rate = RATE_3; tier = "≤6000";
        } else {
            rate = RATE_4; tier = ">6000";
        }
        BigDecimal fee = amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        String amountStr = amount.stripTrailingZeros().toPlainString();
        String ratePct = rate.movePointRight(2).stripTrailingZeros().toPlainString();
        return "【计算（模拟）】押金 " + amountStr + " 元 → 费率档 " + tier + " = " + ratePct + "% → 保函费约 " + fee + " 元。";
    }
}
