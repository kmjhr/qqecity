package com.icbc.qingqi.module.guarantee;

import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.security.UserContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 安居金融风控模块接口（租房履约保函）
 */
@RestController
@RequestMapping("/api/v1/guarantee")
public class GuaranteeController {

    private final GuaranteeService guaranteeService;

    public GuaranteeController(GuaranteeService guaranteeService) {
        this.guaranteeService = guaranteeService;
    }

    @Data
    public static class ApplyRequest {
        @NotBlank(message = "房东姓名不能为空")
        private String landlordName;
        @NotBlank(message = "房东电话不能为空")
        private String landlordPhone;
        @NotBlank(message = "房源地址不能为空")
        private String houseAddress;
        @NotNull(message = "月租金不能为空")
        private BigDecimal monthlyRent;
        @NotNull(message = "押金金额不能为空")
        private BigDecimal depositAmount;
        @NotNull(message = "租期开始不能为空")
        private LocalDate leaseStart;
        @NotNull(message = "租期结束不能为空")
        private LocalDate leaseEnd;
        /** 保函期限（月），默认12 */
        private Integer termMonths;
    }

    @Data
    public static class ConfirmRequest {
        @NotNull(message = "申请单号不能为空")
        private Long applyId;
    }

    /** ①发起申请（含③AI合同复审） */
    @PostMapping("/apply")
    public Result<Map<String, Object>> apply(@Valid @RequestBody ApplyRequest req) {
        LeaseContract contract = new LeaseContract();
        contract.setLandlordName(req.getLandlordName());
        contract.setLandlordPhone(req.getLandlordPhone());
        contract.setHouseAddress(req.getHouseAddress());
        contract.setMonthlyRent(req.getMonthlyRent());
        contract.setDepositAmount(req.getDepositAmount());
        contract.setLeaseStart(req.getLeaseStart());
        contract.setLeaseEnd(req.getLeaseEnd());
        Long applyId = guaranteeService.apply(UserContext.requireUserId(), contract, req.getTermMonths());
        return Result.ok(guaranteeService.detail(applyId));
    }

    /** ②房东在线确认 */
    @PostMapping("/landlord/confirm")
    public Result<Void> confirm(@Valid @RequestBody ConfirmRequest req) {
        guaranteeService.landlordConfirm(req.getApplyId());
        return Result.ok();
    }

    /** ④缴费出函 */
    @PostMapping("/fee/pay")
    public Result<Void> pay(@Valid @RequestBody ConfirmRequest req) {
        guaranteeService.payAndIssue(req.getApplyId());
        return Result.ok();
    }

    /** 保函列表 */
    @GetMapping("/list")
    public Result<List<GuaranteeApply>> list() {
        return Result.ok(guaranteeService.listByUser(UserContext.requireUserId()));
    }

    /** 保函详情 */
    @GetMapping("/{applyId}")
    public Result<Map<String, Object>> detail(@PathVariable Long applyId) {
        return Result.ok(guaranteeService.detail(applyId));
    }
}
