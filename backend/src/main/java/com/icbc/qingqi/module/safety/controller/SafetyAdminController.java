package com.icbc.qingqi.module.safety.controller;

import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.module.safety.entity.BizAntiFraudAlert;
import com.icbc.qingqi.module.safety.entity.BizAntiFraudContent;
import com.icbc.qingqi.module.safety.mapper.BizAntiFraudAlertMapper;
import com.icbc.qingqi.module.safety.mapper.BizAntiFraudContentMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 模块5 - 反诈预警管理端维护
 * <p>
 * 路径 /api/v1/admin/safety/alerts（含 /admin/，JwtAuthFilter 自动校验 ADMIN/BANK_OPERATOR）
 * 实时反诈预警为「人工维护·模拟实时」，管理端可新增/上下架/删除预警
 * <p>
 * 反诈教学内容（biz_anti_fraud_content）管理端维护：/api/v1/admin/safety/contents
 */
@Tag(name = "反诈预警管理（管理端）")
@RestController
@RequestMapping("/v1/admin/safety")
public class SafetyAdminController {

    private final BizAntiFraudAlertMapper alertMapper;
    private final BizAntiFraudContentMapper contentMapper;

    public SafetyAdminController(BizAntiFraudAlertMapper alertMapper, BizAntiFraudContentMapper contentMapper) {
        this.alertMapper = alertMapper;
        this.contentMapper = contentMapper;
    }

    @Operation(summary = "预警列表（含未发布）")
    @GetMapping("/alerts")
    public Result<java.util.List<BizAntiFraudAlert>> list() {
        return Result.success(alertMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BizAntiFraudAlert>()
                .orderByDesc(BizAntiFraudAlert::getPublishTime)));
    }

    @Operation(summary = "新增预警")
    @PostMapping("/alerts")
    public Result<BizAntiFraudAlert> create(@RequestBody BizAntiFraudAlert alert) {
        if (alert.getStatus() == null) alert.setStatus(1);
        // publish_time 表结构 NOT NULL 无默认值，缺省时补当前时间（修复新增预警必 5000 的问题）
        if (alert.getPublishTime() == null) alert.setPublishTime(LocalDateTime.now());
        alertMapper.insert(alert);
        return Result.success(alert);
    }

    @Operation(summary = "更新预警")
    @PutMapping("/alerts/{id}")
    public Result<BizAntiFraudAlert> update(@PathVariable Long id, @RequestBody BizAntiFraudAlert alert) {
        alert.setId(id);
        alertMapper.updateById(alert);
        return Result.success(alertMapper.selectById(id));
    }

    @Operation(summary = "下架/上架预警")
    @PutMapping("/alerts/{id}/status")
    public Result<BizAntiFraudAlert> toggle(@PathVariable Long id, @RequestParam Integer status) {
        BizAntiFraudAlert a = new BizAntiFraudAlert();
        a.setId(id);
        a.setStatus(status);
        alertMapper.updateById(a);
        return Result.success(alertMapper.selectById(id));
    }

    @Operation(summary = "删除预警")
    @DeleteMapping("/alerts/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        alertMapper.deleteById(id);
        return Result.success(null);
    }

    // ============================================================
    //  反诈教学内容管理（biz_anti_fraud_content）
    // ============================================================

    @Operation(summary = "反诈教学内容列表（含未发布）")
    @GetMapping("/contents")
    public Result<java.util.List<BizAntiFraudContent>> listContents() {
        return Result.success(contentMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BizAntiFraudContent>()
                        .orderByDesc(BizAntiFraudContent::getPublishTime)));
    }

    @Operation(summary = "新增反诈教学内容")
    @PostMapping("/contents")
    public Result<BizAntiFraudContent> createContent(@RequestBody BizAntiFraudContent content) {
        if (content.getStatus() == null) content.setStatus(1);
        if (content.getViewCount() == null) content.setViewCount(0);
        contentMapper.insert(content);
        return Result.success(contentMapper.selectById(content.getId()));
    }

    @Operation(summary = "更新反诈教学内容")
    @PutMapping("/contents/{id}")
    public Result<BizAntiFraudContent> updateContent(@PathVariable Long id, @RequestBody BizAntiFraudContent content) {
        content.setId(id);
        contentMapper.updateById(content);
        return Result.success(contentMapper.selectById(id));
    }

    @Operation(summary = "发布/下架反诈教学内容")
    @PutMapping("/contents/{id}/status")
    public Result<BizAntiFraudContent> toggleContent(@PathVariable Long id, @RequestParam Integer status) {
        BizAntiFraudContent c = new BizAntiFraudContent();
        c.setId(id);
        c.setStatus(status);
        contentMapper.updateById(c);
        return Result.success(contentMapper.selectById(id));
    }

    @Operation(summary = "删除反诈教学内容")
    @DeleteMapping("/contents/{id}")
    public Result<Void> deleteContent(@PathVariable Long id) {
        contentMapper.deleteById(id);
        return Result.success(null);
    }
}
