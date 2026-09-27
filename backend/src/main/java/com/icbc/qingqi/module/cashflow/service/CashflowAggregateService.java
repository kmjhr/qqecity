package com.icbc.qingqi.module.cashflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icbc.qingqi.module.cashflow.dto.AggregateReportVO;
import com.icbc.qingqi.module.cashflow.dto.AggregateTxnVO;
import com.icbc.qingqi.module.cashflow.dto.ChannelAuthDTO;
import com.icbc.qingqi.module.cashflow.dto.ChannelAuthVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 多渠道流水聚合服务（演示级，模拟）
 * <p>
 * 数据不入库（符合"流水聚合可复用 biz_transaction 扩展 source_channel，不单独建表"原则）
 * 授权与流水均存 Redis：
 * - key: cashflow:auth:{userId}     → 授权渠道列表 JSON
 * - key: cashflow:aggregate:{userId} → 聚合流水 JSON（TTL 24h，解绑即清）
 * <p>
 * 来源标注"已授权聚合"，全程"模拟"口径
 */
@Slf4j
@Service
public class CashflowAggregateService {

    private static final Duration AUTH_TTL = Duration.ofDays(7);
    private static final Duration AGG_TTL = Duration.ofHours(24);

    /** 支持渠道 */
    private static final Map<String, String> CHANNEL_NAMES = new HashMap<>();
    static {
        CHANNEL_NAMES.put("ICBC_QR", "工行收款码");
        CHANNEL_NAMES.put("WECHAT", "微信支付");
        CHANNEL_NAMES.put("ALIPAY", "支付宝");
        CHANNEL_NAMES.put("TAOBAO", "淘宝店");
    }

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final Random rnd = new Random(42);

    public CashflowAggregateService(StringRedisTemplate redis, ObjectMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
    }

    /**
     * 查询当前授权
     */
    public ChannelAuthVO getAuth(Long userId) {
        ChannelAuthVO vo = new ChannelAuthVO();
        vo.setSimulated(true);
        List<ChannelAuthVO.ChannelInfo> list = loadAuth(userId);
        vo.setAuthorizedChannels(list);
        vo.setAuthTime(list.isEmpty() ? null : list.get(0).getAuthAt());
        vo.setNotice("演示口径，所有流水均为模拟生成；授权可随时解绑，解绑后聚合流水立即清除。");
        return vo;
    }

    /**
     * 授权勾选渠道 → 生成聚合流水入缓存
     */
    public ChannelAuthVO authorize(Long userId, ChannelAuthDTO dto) {
        // 校验渠道
        Set<String> valid = new HashSet<>(CHANNEL_NAMES.keySet());
        for (String c : dto.getChannels()) {
            if (!valid.contains(c)) {
                throw new IllegalArgumentException("不支持的渠道：" + c + "（支持：ICBC_QR/WECHAT/ALIPAY/TAOBAO）");
            }
        }
        LocalDateTime now = LocalDateTime.now();
        List<ChannelAuthVO.ChannelInfo> infos = dto.getChannels().stream()
                .map(c -> {
                    ChannelAuthVO.ChannelInfo info = new ChannelAuthVO.ChannelInfo();
                    info.setChannelCode(c);
                    info.setChannelName(CHANNEL_NAMES.get(c));
                    info.setAuthAt(now);
                    info.setSimulated(true);
                    return info;
                })
                .collect(Collectors.toList());

        // 保存授权
        try {
            redis.opsForValue().set(authKey(userId), mapper.writeValueAsString(infos), AUTH_TTL);
        } catch (Exception e) {
            log.warn("[CashflowAuth] 写入失败 userId={}：{}", userId, e.getMessage());
        }

        // 生成模拟聚合流水
        List<AggregateTxnVO> txns = generateMockTxns(dto.getChannels(), now);

        // 保存流水
        try {
            redis.opsForValue().set(aggKey(userId), mapper.writeValueAsString(txns), AGG_TTL);
        } catch (Exception e) {
            log.warn("[CashflowAgg] 写入失败 userId={}：{}", userId, e.getMessage());
        }

        ChannelAuthVO vo = new ChannelAuthVO();
        vo.setAuthorizedChannels(infos);
        vo.setAuthTime(now);
        vo.setSimulated(true);
        vo.setNotice("已授权 " + infos.size() + " 个渠道并聚合流水（模拟），TTL 24h。可解绑清除。");
        return vo;
    }

    /**
     * 解绑指定渠道（清除该渠道流水，全解则清空聚合缓存）
     */
    public ChannelAuthVO unbind(Long userId, String channelCode) {
        List<ChannelAuthVO.ChannelInfo> list = loadAuth(userId);
        List<ChannelAuthVO.ChannelInfo> remain = list.stream()
                .filter(c -> !c.getChannelCode().equals(channelCode))
                .collect(Collectors.toList());
        if (remain.isEmpty()) {
            redis.delete(authKey(userId));
            redis.delete(aggKey(userId));
            ChannelAuthVO vo = new ChannelAuthVO();
            vo.setAuthorizedChannels(new ArrayList<>());
            vo.setSimulated(true);
            vo.setNotice("渠道 " + channelCode + " 已解绑，无剩余授权渠道，聚合流水已清除。");
            return vo;
        }
        // 重新生成（仅剩余渠道）
        try {
            redis.opsForValue().set(authKey(userId), mapper.writeValueAsString(remain), AUTH_TTL);
        } catch (Exception e) {
            log.warn("[CashflowAuth] 更新失败 userId={}：{}", userId, e.getMessage());
        }
        List<AggregateTxnVO> txns = generateMockTxns(
                remain.stream().map(ChannelAuthVO.ChannelInfo::getChannelCode).collect(Collectors.toList()),
                LocalDateTime.now());
        try {
            redis.opsForValue().set(aggKey(userId), mapper.writeValueAsString(txns), AGG_TTL);
        } catch (Exception e) {
            log.warn("[CashflowAgg] 更新失败 userId={}：{}", userId, e.getMessage());
        }
        ChannelAuthVO vo = new ChannelAuthVO();
        vo.setAuthorizedChannels(remain);
        vo.setSimulated(true);
        vo.setNotice("渠道 " + channelCode + " 已解绑，剩余 " + remain.size() + " 个渠道，聚合流水已刷新。");
        return vo;
    }

    /**
     * 拉取聚合报表
     */
    public AggregateReportVO getReport(Long userId) {
        List<AggregateTxnVO> txns = loadAggregate(userId);
        AggregateReportVO vo = new AggregateReportVO();
        vo.setSimulated(true);
        if (txns.isEmpty()) {
            vo.setTotalCount(0);
            vo.setTotalIncome(BigDecimal.ZERO);
            vo.setTotalRefund(BigDecimal.ZERO);
            vo.setNetAmount(BigDecimal.ZERO);
            vo.setChannelSummaries(new ArrayList<>());
            vo.setRecords(new ArrayList<>());
            return vo;
        }

        vo.setTotalCount(txns.size());
        BigDecimal income = txns.stream()
                .filter(t -> "INCOME".equals(t.getTxnType()))
                .map(AggregateTxnVO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refund = txns.stream()
                .filter(t -> "REFUND".equals(t.getTxnType()))
                .map(AggregateTxnVO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setTotalIncome(income);
        vo.setTotalRefund(refund);
        vo.setNetAmount(income.subtract(refund));

        // 渠道汇总
        Map<String, List<AggregateTxnVO>> byChannel = txns.stream()
                .collect(Collectors.groupingBy(AggregateTxnVO::getChannelCode));
        List<AggregateReportVO.ChannelSummary> sums = new ArrayList<>();
        for (Map.Entry<String, List<AggregateTxnVO>> e : byChannel.entrySet()) {
            AggregateReportVO.ChannelSummary s = new AggregateReportVO.ChannelSummary();
            s.setChannelCode(e.getKey());
            s.setChannelName(CHANNEL_NAMES.getOrDefault(e.getKey(), e.getKey()));
            s.setTxnCount(e.getValue().size());
            s.setIncome(e.getValue().stream()
                    .filter(t -> "INCOME".equals(t.getTxnType()))
                    .map(AggregateTxnVO::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            s.setRefund(e.getValue().stream()
                    .filter(t -> "REFUND".equals(t.getTxnType()))
                    .map(AggregateTxnVO::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            sums.add(s);
        }
        vo.setChannelSummaries(sums);

        // 最近 50 条（按时间倒序）
        List<AggregateTxnVO> recent = txns.stream()
                .sorted((a, b) -> b.getTxnTime().compareTo(a.getTxnTime()))
                .limit(50)
                .collect(Collectors.toList());
        vo.setRecords(recent);
        return vo;
    }

    /**
     * 供防刷单服务调用：读取原始聚合流水
     */
    public List<AggregateTxnVO> loadRaw(Long userId) {
        return loadAggregate(userId);
    }

    // ============================================================
    // 内部工具
    // ============================================================

    private String authKey(Long userId) { return "cashflow:auth:" + userId; }
    private String aggKey(Long userId)  { return "cashflow:aggregate:" + userId; }

    private List<ChannelAuthVO.ChannelInfo> loadAuth(Long userId) {
        try {
            String json = redis.opsForValue().get(authKey(userId));
            if (json == null || json.isBlank()) return new ArrayList<>();
            return mapper.readValue(json, new TypeReference<List<ChannelAuthVO.ChannelInfo>>() {});
        } catch (Exception e) {
            log.warn("[CashflowAuth] 读取失败 userId={}：{}", userId, e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<AggregateTxnVO> loadAggregate(Long userId) {
        try {
            String json = redis.opsForValue().get(aggKey(userId));
            if (json == null || json.isBlank()) return new ArrayList<>();
            return mapper.readValue(json, new TypeReference<List<AggregateTxnVO>>() {});
        } catch (Exception e) {
            log.warn("[CashflowAgg] 读取失败 userId={}：{}", userId, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 模拟生成流水（每渠道 8-15 条；90% INCOME / 10% REFUND）
     */
    private List<AggregateTxnVO> generateMockTxns(List<String> channels, LocalDateTime authTime) {
        List<AggregateTxnVO> list = new ArrayList<>();
        int seq = 0;
        for (String ch : channels) {
            int n = 8 + rnd.nextInt(8); // 8-15 条
            for (int i = 0; i < n; i++) {
                AggregateTxnVO t = new AggregateTxnVO();
                t.setTxnNo("AG" + String.format("%06d", ++seq));
                t.setChannelCode(ch);
                t.setChannelName(CHANNEL_NAMES.get(ch));
                t.setCounterparty("买家**" + (1000 + rnd.nextInt(9000)));
                boolean isRefund = rnd.nextDouble() < 0.1;
                t.setTxnType(isRefund ? "REFUND" : "INCOME");
                BigDecimal amount;
                if (isRefund) {
                    amount = new BigDecimal(50 + rnd.nextInt(300)).setScale(2);
                } else {
                    amount = new BigDecimal(20 + rnd.nextInt(980)).setScale(2);
                }
                t.setAmount(amount);
                // 过去 30 天内随机时间
                LocalDateTime time = authTime.minusDays(rnd.nextInt(30)).minusHours(rnd.nextInt(24));
                t.setTxnTime(time);
                t.setDescription(isRefund ? "退款-订单售后" : "商品销售-订单" + (10000 + rnd.nextInt(90000)));
                t.setSourceTag("已授权聚合");
                t.setSimulated(true);
                list.add(t);
            }
        }
        // 按时间正序
        list.sort(java.util.Comparator.comparing(AggregateTxnVO::getTxnTime));
        return list;
    }
}
