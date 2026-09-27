package com.icbc.qingqi.module.operation.service;

import com.icbc.qingqi.common.BizException;
import com.icbc.qingqi.common.ErrorCode;
import com.icbc.qingqi.module.operation.dto.ChecklistItemVO;
import com.icbc.qingqi.module.operation.dto.FinanceContentVO;
import com.icbc.qingqi.module.operation.dto.IndividualToCompanyChecklistDTO;
import com.icbc.qingqi.module.operation.dto.IndividualToCompanyResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 财税科普 + 个转企引导服务
 * <p>
 * 缺口 #25 财税科普与个转企引导（简化）
 * 内容硬编码返回（轻量化，不新增表）
 * 合规口径：内容仅为科普参考，不构成税务/法律意见
 */
@Slf4j
@Service
public class FinanceContentService {

    private static final String COMPLIANCE_NOTICE =
            "本内容仅为财税科普参考，不构成税务/法律意见。具体申报与筹划请咨询持证税务师或专业律师。";

    // ============================================================
    //  财税科普内容
    // ============================================================

    /**
     * 按分类查询财税科普内容
     *
     * @param category TAX_POPULARIZATION-税务优惠 / INVOICE_GUIDE-发票常识；不传则返回全部
     */
    public List<FinanceContentVO> listByCategory(String category) {
        List<FinanceContentVO> all = new ArrayList<>();
        all.addAll(buildTaxPopularizationContent());
        all.addAll(buildInvoiceGuideContent());

        if (category == null || category.isEmpty()) {
            return all;
        }
        return all.stream().filter(c -> category.equals(c.getCategory())).toList();
    }

    /**
     * 内容详情
     */
    public FinanceContentVO detail(Long id) {
        List<FinanceContentVO> all = new ArrayList<>();
        all.addAll(buildTaxPopularizationContent());
        all.addAll(buildInvoiceGuideContent());
        return all.stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new BizException(ErrorCode.BIZ_RULE_NOT_MET, "内容不存在"));
    }

    // ============================================================
    //  个转企引导
    // ============================================================

    /**
     * 获取个转企自查条件清单（供前端渲染问卷）
     */
    public List<ChecklistItemVO> getChecklist() {
        return List.of(
                buildChecklistItem(1, "年营业收入是否超过 120 万元",
                        "增值税一般纳税人登记门槛为年应税销售额 500 万元，但小规模纳税人月销售额 10 万以内免征增值税；超过 120 万建议评估转企收益。"),
                buildChecklistItem(2, "是否需要对外开具增值税专用发票",
                        "个体工商户可申请代开专票，但部分合作方要求企业自行开具专票。"),
                buildChecklistItem(3, "经营风险是否需要有限责任公司隔离个人财产",
                        "个体工商户承担无限责任；有限公司股东以认缴出资为限承担责任。"),
                buildChecklistItem(4, "是否计划引入股权融资或合伙人",
                        "个体工商户无法发行股权；有限公司便于股权结构和融资。"),
                buildChecklistItem(5, "是否需要做税务筹划（如企业所得税优惠）",
                        "有限公司可享受小型微利企业减按 25% 计入应纳税所得额等优惠；个体工商户按经营所得五级超额累进。"),
                buildChecklistItem(6, "是否有合作伙伴/招标项目要求企业主体",
                        "部分政企招标、平台入驻明确要求营业执照为企业类型。")
        );
    }

    /**
     * 提交自查答案，返回结论
     */
    public IndividualToCompanyResultVO selfCheck(IndividualToCompanyChecklistDTO dto) {
        Map<Integer, Boolean> answers = dto.getAnswers();
        if (answers == null || answers.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "自查答案不能为空");
        }

        List<ChecklistItemVO> checklist = getChecklist();
        List<String> matchedReasons = new ArrayList<>();
        List<String> unmatchedReasons = new ArrayList<>();

        for (ChecklistItemVO item : checklist) {
            Boolean ans = answers.get(item.getId());
            if (Boolean.TRUE.equals(ans)) {
                matchedReasons.add("[" + item.getId() + "] " + item.getTitle() + " — 是");
            } else {
                unmatchedReasons.add("[" + item.getId() + "] " + item.getTitle() + " — 否");
            }
        }

        int matched = matchedReasons.size();
        int total = checklist.size();

        String conclusion;
        String conclusionDesc;
        if (matched >= 4) {
            conclusion = "RECOMMEND";
            conclusionDesc = "建议尽快办理「个转企」。您命中 " + matched + "/" + total
                    + " 项关键条件，转为企业主体可在税务筹划、风险隔离、融资能力上获得明显收益。";
        } else if (matched >= 2) {
            conclusion = "OPTIONAL";
            conclusionDesc = "「个转企」可选。您命中 " + matched + "/" + total
                    + " 项条件，建议结合未来 1-2 年经营规划评估，可暂缓但应提前布局。";
        } else {
            conclusion = "NOT_RECOMMEND";
            conclusionDesc = "暂不建议「个转企」。您命中 " + matched + "/" + total
                    + " 项条件，当前经营规模与需求个体工商户即可满足，转企反而增加合规成本。";
        }

        IndividualToCompanyResultVO vo = new IndividualToCompanyResultVO();
        vo.setMatchedCount(matched);
        vo.setTotalCount(total);
        vo.setConclusion(conclusion);
        vo.setConclusionDesc(conclusionDesc);
        vo.setMatchedReasons(matchedReasons);
        vo.setUnmatchedReasons(unmatchedReasons);
        vo.setNextSteps(buildNextSteps(conclusion));
        vo.setComplianceNotice(COMPLIANCE_NOTICE);

        log.info("[个转企自查] 命中={}/{}, 结论={}", matched, total, conclusion);
        return vo;
    }

    // ============================================================
    //  内容构建（硬编码）
    // ============================================================

    private List<FinanceContentVO> buildTaxPopularizationContent() {
        List<FinanceContentVO> list = new ArrayList<>();

        list.add(buildContent(1L, "TAX_POPULARIZATION", "税务优惠",
                "个体工商户税收优惠速览",
                "月销售额 10 万以内免征增值税，所得减半征收等政策要点。",
                List.of(
                        "根据现行政策，小规模纳税人月销售额未超过 10 万元的，免征增值税（有效期至 2027 年 12 月 31 日）。",
                        "个体工商户经营所得年应纳税所得额不超过 200 万元的部分，在现行优惠政策基础上减半征收个人所得税。",
                        "符合小型微利企业条件的，可享受企业所得税优惠（适用于企业类型，不适用于个体工商户）。"
                ),
                List.of(
                        "月销售额 10 万元是免征增值税的关键门槛",
                        "经营所得减半征收政策有效期至 2027 年底",
                        "个体工商户不享受企业所得税优惠"
                )));
        list.add(buildContent(2L, "TAX_POPULARIZATION", "税务优惠",
                "小规模纳税人vs一般纳税人如何选择",
                "从抵扣、开票、税负三个维度对比两种纳税人身份。",
                List.of(
                        "小规模纳税人：征收率 3%（优惠期 1%），月销售额 10 万以内免征，不能抵扣进项税。",
                        "一般纳税人：税率 6%/9%/13%，可抵扣进项税，可自行开具增值税专用发票。",
                        "选择建议：上游供应商多为一般纳税人（可取得专票）且年销售额 > 500 万，建议登记为一般纳税人。"
                ),
                List.of(
                        "登记门槛：年应税销售额 500 万元",
                        "小规模征收率优惠期 1%（2027 年底前）",
                        "一般纳税人可自行开具专票"
                )));
        list.add(buildContent(3L, "TAX_POPULARIZATION", "税务优惠",
                "「六税两费」减免政策",
                "小规模纳税人、小型微利企业、个体工商户可享受「六税两费」减半征收。",
                List.of(
                        "适用对象：小规模纳税人、小型微利企业、个体工商户。",
                        "减免范围：资源税、城市维护建设税、房产税、城镇土地使用税、印花税（不含证券交易印花税）、耕地占用税和教育费附加、地方教育附加。",
                        "减免幅度：在现行优惠政策基础上减半征收（有效期至 2027 年 12 月 31 日）。"
                ),
                List.of(
                        "减半征收适用「六税两费」",
                        "小规模、小型微利、个体户均适用",
                        "政策有效期至 2027 年底"
                )));
        list.add(buildContent(4L, "TAX_POPULARIZATION", "税务优惠",
                "重点群体创业税费扣减",
                "毕业年度内高校毕业生、脱贫人口等从事个体经营可享受税费扣减。",
                List.of(
                        "适用对象：纳入全国扶贫开发信息系统的脱贫人口、登记失业半年以上人员、零就业家庭/享受城市居民最低生活保障家庭劳动年龄内人员、毕业年度内高校毕业生。",
                        "优惠内容：从事个体经营的，自办理个体工商户登记当月起，在 3 年（36 个月）内按每户每年 20000 元为限额依次扣减增值税、城市维护建设税、教育费附加、地方教育附加和个人所得税。",
                        "政策依据：财税〔2023〕15 号，执行至 2027 年 12 月 31 日。"
                ),
                List.of(
                        "扣减限额每户每年 20000 元",
                        "扣减期限 36 个月",
                        "执行至 2027 年底"
                )));

        return list;
    }

    private List<FinanceContentVO> buildInvoiceGuideContent() {
        List<FinanceContentVO> list = new ArrayList<>();

        list.add(buildContent(101L, "INVOICE_GUIDE", "发票常识",
                "发票基本类型与开具场景",
                "增值税专用发票、普通发票、电子发票的适用场景对比。",
                List.of(
                        "增值税专用发票：可用于进项税抵扣，一般纳税人可自行开具，小规模纳税人可申请代开。",
                        "增值税普通发票：不能抵扣进项税，作为成本费用入账凭证。",
                        "电子发票：与纸质发票同等法律效力，包括增值税电子专用发票、增值税电子普通发票、全面数字化的电子发票（数电票）。"
                ),
                List.of(
                        "专票可抵扣，普票不能抵扣",
                        "电子发票与纸质发票同等效力",
                        "数电票已在全国推广"
                )));
        list.add(buildContent(102L, "INVOICE_GUIDE", "发票常识",
                "发票开具时点与风险防范",
                "纳税义务发生时间、虚开发票风险、红冲规则。",
                List.of(
                        "开具时点：发生增值税纳税义务时开具，一般为收讫销售款项或取得索取销售款项凭据的当天；先开具发票的，为开具发票的当天。",
                        "虚开发票风险：为他人/自己/介绍他人开具与实际经营业务情况不符的发票，构成虚开发票，可能面临行政处罚乃至刑事责任。",
                        "红冲规则：发生销货退回、开票有误、应税服务中止等情形，需要开具红字发票冲回原蓝字发票。"
                ),
                List.of(
                        "纳税义务发生时开具发票",
                        "虚开发票可入刑，切勿触碰",
                        "红冲需先填开《红字发票信息表》"
                )));
        list.add(buildContent(103L, "INVOICE_GUIDE", "发票常识",
                "电子发票查验与归档",
                "全国增值税发票查验平台、电子凭证归档规范。",
                List.of(
                        "查验渠道：全国增值税发票查验平台（https://inv-veri.chinatax.gov.cn），支持 5 年内开具的增值税发票查验。",
                        "归档要求：单位电子发票应按《会计档案管理办法》（财政部 国家档案局令第 79 号）归档，可仅以电子形式保存。",
                        "重复报销防范：建议建立电子发票台账或使用 OCR 识别+查重工具，避免同一发票多次报销。"
                ),
                List.of(
                        "官方查验平台：inv-veri.chinatax.gov.cn",
                        "电子发票可仅以电子形式归档",
                        "需建立查重机制防范重复报销"
                )));

        return list;
    }

    private FinanceContentVO buildContent(Long id, String category, String categoryName,
                                          String title, String summary,
                                          List<String> content, List<String> keyPoints) {
        FinanceContentVO vo = new FinanceContentVO();
        vo.setId(id);
        vo.setCategory(category);
        vo.setCategoryName(categoryName);
        vo.setTitle(title);
        vo.setSummary(summary);
        vo.setContent(content);
        vo.setKeyPoints(keyPoints);
        vo.setComplianceNotice(COMPLIANCE_NOTICE);
        return vo;
    }

    private ChecklistItemVO buildChecklistItem(Integer id, String title, String description) {
        ChecklistItemVO vo = new ChecklistItemVO();
        vo.setId(id);
        vo.setTitle(title);
        vo.setDescription(description);
        vo.setYesLabel("是");
        vo.setNoLabel("否");
        return vo;
    }

    private List<String> buildNextSteps(String conclusion) {
        List<String> steps = new ArrayList<>();
        switch (conclusion) {
            case "RECOMMEND" -> {
                steps.add("1. 准备材料：营业执照、税务登记证、近期财务报表、印章等");
                steps.add("2. 办理路径：先到市场监管部门办理「个转企」登记，再到税务部门办理相关变更");
                steps.add("3. 同步事项：银行账户变更、社保公积金主体变更、合同主体迁移");
                steps.add("4. 政策咨询：可前往当地市场监管局或拨打 12345 咨询（演示用，请以官方为准）");
            }
            case "OPTIONAL" -> {
                steps.add("1. 暂可维持个体工商户身份，但建议提前规划企业架构");
                steps.add("2. 关注未来 1-2 年的营业收入、合作方要求变化");
                steps.add("3. 建议定期复盘（如每季度），命中条件增加时再启动转企");
            }
            case "NOT_RECOMMEND" -> {
                steps.add("1. 维持个体工商户身份，专注经营规模扩大");
                steps.add("2. 关注月销售额是否突破 10 万元增值税免征门槛");
                steps.add("3. 留意合作伙伴是否提出企业主体要求，再评估转企时机");
            }
        }
        return steps;
    }
}
