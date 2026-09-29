package com.icbc.qingqi.module.guarantee.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.module.guarantee.dto.LandlordUpdateDTO;
import com.icbc.qingqi.module.guarantee.dto.LandlordVO;
import com.icbc.qingqi.module.guarantee.service.LandlordAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端 - 房东管理分区
 * <p>
 * 路径：/api/v1/admin/landlord/** （JwtAuthFilter isAdminPath 校验 ADMIN/banker 角色）
 * <p>
 * 房东为独立业务主体（biz_landlord，类似商户而非普通用户），
 * 用户列表不再混入房东，房东在本分区统一管理。
 */
@Tag(name = "管理端-房东管理")
@RestController
@RequestMapping("/v1/admin/landlord")
public class LandlordAdminController {

    private final LandlordAdminService landlordAdminService;

    public LandlordAdminController(LandlordAdminService landlordAdminService) {
        this.landlordAdminService = landlordAdminService;
    }

    @Operation(summary = "房东分页列表",
            description = "keyword：姓名/电话/证件号；verifyStatus：PENDING/VERIFIED/REJECTED（默认全部）")
    @GetMapping("/page")
    public Result<Page<LandlordVO>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String verifyStatus) {
        return Result.success(landlordAdminService.page(pageNum, pageSize, keyword, verifyStatus));
    }

    @Operation(summary = "房东详情")
    @GetMapping("/{id}")
    public Result<LandlordVO> getById(@PathVariable Long id) {
        return Result.success(landlordAdminService.getById(id));
    }

    @Operation(summary = "编辑房东信息")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody LandlordUpdateDTO dto) {
        landlordAdminService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "认证裁决：VERIFIED-通过 / REJECTED-驳回 / PENDING-待审")
    @PutMapping("/{id}/verify")
    public Result<Void> verify(@PathVariable Long id,
                               @Parameter(description = "VERIFIED/REJECTED/PENDING")
                               @RequestParam String verifyStatus) {
        landlordAdminService.verify(id, verifyStatus);
        return Result.success();
    }

    @Operation(summary = "删除房东（逻辑删除 + 停用关联账号）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        landlordAdminService.delete(id);
        return Result.success();
    }
}
