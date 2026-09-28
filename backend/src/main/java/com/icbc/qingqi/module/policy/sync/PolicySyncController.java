package com.icbc.qingqi.module.policy.sync;

import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.module.policy.sync.dto.PolicySyncResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 政策定时同步管理端接口
 * <p>
 * 路径 /api/v1/admin/policy/sync（含 /admin/，JwtAuthFilter 自动校验 ADMIN/BANK_OPERATOR 角色）
 * 用途：评审演示时手动触发一次官方源同步，返回各源新增/跳过/失败明细
 */
@Tag(name = "政策定时同步（管理端）")
@RestController
@RequestMapping("/v1/admin/policy")
public class PolicySyncController {

    private final PolicySyncTask policySyncTask;

    public PolicySyncController(PolicySyncTask policySyncTask) {
        this.policySyncTask = policySyncTask;
    }

    @Operation(summary = "手动触发政策同步（抓取官方栏目源）",
            description = "立即抓取配置的官方政策栏目页，按申报链接去重入库，返回各源新增/跳过/失败明细。")
    @PostMapping("/sync")
    public Result<List<PolicySyncResult>> sync() {
        return Result.success(policySyncTask.syncNow());
    }
}
