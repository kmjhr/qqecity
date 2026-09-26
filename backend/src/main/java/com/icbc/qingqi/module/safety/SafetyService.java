package com.icbc.qingqi.module.safety;

import com.icbc.qingqi.common.BizException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 模块5 青年金融安全：反诈教学与骗局甄别（规则模拟）
 */
@Service
public class SafetyService {

    private final AntiFraudRecordMapper recordMapper;

    public SafetyService(AntiFraudRecordMapper recordMapper) {
        this.recordMapper = recordMapper;
    }

    /** 反诈教学内容（演示静态内容） */
    public List<Map<String, String>> fraudContent() {
        return List.of(
                Map.of("id", "C001", "title", "警惕「征信修复」骗局",
                        "summary", "个人征信由人民银行统一管理，任何声称可以修复、洗白征信的都是诈骗。"),
                Map.of("id", "C002", "title", "识别「刷单返利」陷阱",
                        "summary", "先垫付、再返利的刷单任务均为诈骗，切勿向陌生账户垫付资金。"),
                Map.of("id", "C003", "title", "防范「套路贷」与高利贷",
                        "summary", "正规贷款不会以任何名义提前收取费用；遇到强制下款、砍头息应停止使用并举报。"),
                Map.of("id", "C004", "title", "拒绝「校园贷」与过度借贷",
                        "summary", "理性消费、量入为出，避免多头借贷与以贷养贷。"));
    }

    /**
     * 话术骗局甄别（关键词规则模拟），返回命中风险列表
     */
    public List<Map<String, String>> verifyText(String text) {
        if (text == null || text.isBlank()) {
            throw new BizException(1001, "话术内容不能为空");
        }
        List<Map<String, String>> hits = new ArrayList<>();
        checkHit(text, hits, new String[]{"征信修复", "征信洗白", "逾期消除"}, "征信修复骗局",
                "声称可付费修复/洗白征信记录，属于诈骗，请勿转账。");
        checkHit(text, hits, new String[]{"刷单", "刷流水", "垫付返利"}, "刷单返利骗局",
                "先垫付后返利是典型刷单诈骗话术。");
        checkHit(text, hits, new String[]{"内部渠道", "稳赚不赔", "高额回报"}, "虚假投资/杀猪盘",
                "承诺高额回报、内部渠道的多为非法集资或杀猪盘。");
        checkHit(text, hits, new String[]{"零门槛贷款", "不看征信", "强制下款"}, "套路贷",
                "零门槛、不看征信并提前收费的贷款多为套路贷。");
        checkHit(text, hits, new String[]{"先交手续费", "中奖需缴税"}, "中奖/手续费诈骗",
                "先交钱后兑奖/放款均为诈骗话术。");
        return hits;
    }

    private void checkHit(String text, List<Map<String, String>> hits,
                          String[] keywords, String type, String advice) {
        for (String k : keywords) {
            if (text.contains(k)) {
                hits.add(Map.of("type", type, "keyword", k, "advice", advice));
                return;
            }
        }
    }

    /** 记录一次反诈学习完成 */
    public Long recordLearn(Long userId, String learnType, String contentId, Integer score) {
        AntiFraudRecord record = new AntiFraudRecord();
        record.setUserId(userId);
        record.setLearnType(learnType);
        record.setContentId(contentId);
        record.setFinishStatus(1);
        record.setScore(score);
        record.setLearnTime(LocalDateTime.now());
        recordMapper.insert(record);
        return record.getRecordId();
    }
}
