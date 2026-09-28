package com.icbc.qingqi.module.safety.controller;

import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.module.safety.entity.BizAntiFraudAlert;
import com.icbc.qingqi.module.safety.mapper.BizAntiFraudAlertMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * 模块5 - 反诈预警管理端维护
 * <p>
 * 路径 /api/v1/admin/safety/alerts（含 /admin/，JwtAuthFilter 自动校验 ADMIN/BANK_OPERATOR）
 * 实时反诈预警为「人工维护·模拟实时」，管理端可新增/上下架/删除预警
 */
@Tag(name = "反诈预警管理（管理端）")
@RestController
@RequestMapping("/v1/admin/safety/alerts")
public class SafetyAdminController {

    private final BizAntiFraudAlertMapper alertMapper;

    public SafetyAdminController(BizAntiFraudAlertMapper alertMapper) {
        this.alertMapper = alertMapper;
    }

    @Operation(summary = "预警列表（含未发布）")
    @GetMapping
    public Result<java.util.List<BizAntiFraudAlert>> list() {
        return Result.success(alertMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BizAntiFraudAlert>()
                .orderByDesc(BizAntiFraudAlert::getPublishTime)));
    }

    @Operation(summary = "新增预警")
    @PostMapping
    public Result<BizAntiFraudAlert> create(@RequestBody BizAntiFraudAlert alert) {
        if (alert.getStatus() == null) alert.setStatus(1);
        alertMapper.insert(alert);
        return Result.success(alert);
    }

    @Operation(summary = "更新预警")
    @PutMapping("/{id}")
    public Result<BizAntiFraudAlert> update(@PathVariable Long id, @RequestBody BizAntiFraudAlert alert) {
        alert.setId(id);
        alertMapper.updateById(alert);
        return Result.success(alertMapper.selectById(id));
    }

    @Operation(summary = "下架/上架预警")
    @PutMapping("/{id}/status")
    public Result<BizAntiFraudAlert> toggle(@PathVariable Long id, @RequestParam Integer status) {
        BizAntiFraudAlert a = new BizAntiFraudAlert();
        a.setId(id);
        a.setStatus(status);
        alertMapper.updateById(a);
        return Result.success(alertMapper.selectById(id));
    }

    @Operation(summary = "删除预警")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        alertMapper.deleteById(id);
        return Result.success(null);
    }
}
