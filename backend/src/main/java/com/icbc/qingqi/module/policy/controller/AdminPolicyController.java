package com.icbc.qingqi.module.policy.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.module.policy.entity.BizPolicyPortal;
import com.icbc.qingqi.module.policy.mapper.BizPolicyPortalMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 政策专区 - 官方入口导航管理端维护
 * <p>
 * 路径 /api/v1/admin/policy/portals（含 /admin/，JwtAuthFilter 自动校验 ADMIN/BANK_OPERATOR）
 * 管理端可维护官方政策入口（人社/住建/政务/税务/教育等），用户端政策专区实时展示
 */
@Tag(name = "政策门户管理（管理端）")
@RestController
@RequestMapping("/v1/admin/policy/portals")
public class AdminPolicyController {

    private final BizPolicyPortalMapper portalMapper;

    public AdminPolicyController(BizPolicyPortalMapper portalMapper) {
        this.portalMapper = portalMapper;
    }

    @Operation(summary = "政策门户列表（可按类型/地区筛选）")
    @GetMapping
    public Result<List<BizPolicyPortal>> list(
            @RequestParam(required = false) String portalType,
            @RequestParam(required = false) String region) {
        LambdaQueryWrapper<BizPolicyPortal> wrapper = new LambdaQueryWrapper<>();
        if (portalType != null && !portalType.isBlank()) {
            wrapper.eq(BizPolicyPortal::getPortalType, portalType);
        }
        if (region != null && !region.isBlank()) {
            wrapper.like(BizPolicyPortal::getRegion, region);
        }
        wrapper.orderByAsc(BizPolicyPortal::getSortOrder)
                .orderByAsc(BizPolicyPortal::getId);
        return Result.success(portalMapper.selectList(wrapper));
    }

    @Operation(summary = "新增政策门户")
    @PostMapping
    public Result<BizPolicyPortal> create(@RequestBody BizPolicyPortal portal) {
        if (portal.getStatus() == null) portal.setStatus("ACTIVE");
        if (portal.getSortOrder() == null) portal.setSortOrder(0);
        portalMapper.insert(portal);
        return Result.success(portalMapper.selectById(portal.getId()));
    }

    @Operation(summary = "更新政策门户")
    @PutMapping("/{id}")
    public Result<BizPolicyPortal> update(@PathVariable Long id, @RequestBody BizPolicyPortal portal) {
        portal.setId(id);
        portalMapper.updateById(portal);
        return Result.success(portalMapper.selectById(id));
    }

    @Operation(summary = "启用/停用政策门户")
    @PutMapping("/{id}/status")
    public Result<BizPolicyPortal> toggle(@PathVariable Long id, @RequestParam String status) {
        BizPolicyPortal p = new BizPolicyPortal();
        p.setId(id);
        p.setStatus("ACTIVE".equals(status) ? "ACTIVE" : "INACTIVE");
        portalMapper.updateById(p);
        return Result.success(portalMapper.selectById(id));
    }

    @Operation(summary = "删除政策门户")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        portalMapper.deleteById(id);
        return Result.success(null);
    }
}
