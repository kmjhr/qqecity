package com.icbc.qingqi.module.guarantee.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.module.guarantee.dto.GuaranteeApplyDTO;
import com.icbc.qingqi.module.guarantee.dto.GuaranteeApplicationVO;
import com.icbc.qingqi.module.guarantee.dto.GuaranteeVO;
import com.icbc.qingqi.module.guarantee.dto.LandlordSignDTO;
import com.icbc.qingqi.module.guarantee.service.GuaranteeService;
import com.icbc.qingqi.module.pay.dto.PayOrderVO;
import com.icbc.qingqi.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 模块1 - 安居金融风控（保函全流程）
 * <p>
 * 路径：/api/v1/guarantee/**
 * 覆盖 G-1 ~ G-5：保函申请 → 房东确认 → AI 复审 → 缴费出函 → 查询/状态流转
 * <p>
 * 所有银行能力（开函、缴费、AI 复审）均为模拟桩，演示数据标注"模拟"。
 */
@Tag(name = "安居金融风控 - 保函")
@RestController
@RequestMapping("/v1/guarantee")
public class GuaranteeController {

    private final GuaranteeService guaranteeService;

    public GuaranteeController(GuaranteeService guaranteeService) {
        this.guaranteeService = guaranteeService;
    }

    // ============================================================
    //  G-1 保函申请
    // ============================================================

    @Operation(summary = "G-1 提交保函申请",
            description = "录入房东信息、租赁要素、押金金额，系统自动创建房东/房屋/租赁合同/保函申请记录。")
    @PostMapping("/apply")
    public Result<GuaranteeApplicationVO> apply(@Valid @RequestBody GuaranteeApplyDTO dto) {
        Long userId = UserContext.getUserId();
        return Result.success(guaranteeService.apply(userId, dto));
    }

    // ============================================================
    //  G-2 房东确认（landlord01 演示账号）
    // ============================================================

    @Operation(summary = "G-2 房东在线确认与电子签署",
            description = "房东（演示账号 landlord01）确认保函申请并电子签署（Canvas 手写或点击确认），签署时间/签名入库。确认后自动触发 AI 合同复审。")
    @PutMapping("/{id}/landlord-confirm")
    public Result<GuaranteeApplicationVO> landlordConfirm(
            @Parameter(description = "保函申请 ID") @PathVariable Long id,
            @Valid @RequestBody(required = false) LandlordSignDTO signDTO) {
        Long userId = UserContext.getUserId();
        // 若未传签名，默认点击确认
        LandlordSignDTO dto = signDTO != null ? signDTO : new LandlordSignDTO();
        if (dto.getSignContent() == null || dto.getSignContent().isBlank()) {
            dto.setSignContent("CLICK_CONFIRM");
        }
        return Result.success(guaranteeService.landlordConfirm(userId, id, dto));
    }

    // ============================================================
    //  G-3-补 人工复审（banker 对 AI 转人工的申请做出裁决）
    // ============================================================

    @Operation(summary = "G-3-补 banker 人工复审保函申请",
            description = "banker 对 AI 复审转人工（MANUAL_REVIEW）的申请做出裁决：APPROVED 放行至待缴费 / REJECTED 拒绝。")
    @PutMapping("/{id}/manual-review")
    public Result<GuaranteeApplicationVO> manualReview(
            @Parameter(description = "保函申请 ID") @PathVariable Long id,
            @RequestParam String decision,
            @RequestParam(required = false) String rejectReason) {
        Long userId = UserContext.getUserId();
        return Result.success(guaranteeService.manualReviewApplication(userId, id, decision, rejectReason));
    }

    // ============================================================
    //  G-4 缴费出函
    // ============================================================

    @Operation(summary = "G-4 创建保函费支付订单（收银台支付成功后自动开立电子保函）",
            description = "租客在收银台缴纳保函费（费率 0.8%—1.5%，模拟支付），支付成功自动开立电子保函并推送房东。")
    @PostMapping("/{id}/pay")
    public Result<PayOrderVO> pay(@Parameter(description = "保函申请 ID") @PathVariable Long id) {
        Long userId = UserContext.getUserId();
        return Result.success(guaranteeService.createPayOrder(userId, id));
    }

    // ============================================================
    //  G-5 列表 / 详情
    // ============================================================

    @Operation(summary = "G-5 分页查询保函申请列表",
            description = "当前用户可见：租客看到自己提交的申请，房东看到自己作为房东的申请。")
    @GetMapping("/page")
    public Result<Page<GuaranteeApplicationVO>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status) {
        Long userId = UserContext.getUserId();
        return Result.success(guaranteeService.pageApplications(userId, pageNum, pageSize, status));
    }

    @Operation(summary = "G-5 查询保函申请详情")
    @GetMapping("/{id}")
    public Result<GuaranteeApplicationVO> detail(
            @Parameter(description = "保函申请 ID") @PathVariable Long id) {
        Long userId = UserContext.getUserId();
        return Result.success(guaranteeService.getApplicationDetail(userId, id));
    }

    @Operation(summary = "G-5 查询电子保函详情")
    @GetMapping("/guarantee/{id}")
    public Result<GuaranteeVO> guaranteeDetail(
            @Parameter(description = "电子保函 ID") @PathVariable Long id) {
        Long userId = UserContext.getUserId();
        return Result.success(guaranteeService.getGuaranteeDetail(userId, id));
    }

    // ============================================================
    //  辅助：状态流转说明
    // ============================================================

    @Operation(summary = "保函状态流转说明",
            description = "返回 5 态流转：申请中→待确认→待缴费→已开立→已失效，供前端展示。")
    @GetMapping("/status-flow")
    public Result<Map<String, Object>> statusFlow() {
        List<Map<String, String>> flow = List.of(
                Map.of("status", "SUBMITTED", "name", "申请中", "desc", "租客已提交申请"),
                Map.of("status", "LANDLORD_CONFIRM", "name", "待确认", "desc", "等待房东在线确认与签署"),
                Map.of("status", "PENDING_PAY", "name", "待缴费", "desc", "AI 复审通过，等待租客缴纳保函费"),
                Map.of("status", "APPROVED", "name", "已开立", "desc", "缴费成功，电子保函已开立"),
                Map.of("status", "EXPIRED", "name", "已失效", "desc", "保函到期失效")
        );
        return Result.success(Map.of("flow", flow, "remark", "演示系统，银行能力均为模拟桩"));
    }
}
