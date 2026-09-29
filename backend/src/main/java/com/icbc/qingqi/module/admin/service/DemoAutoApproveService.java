package com.icbc.qingqi.module.admin.service;

import org.springframework.stereotype.Service;

/**
 * 演示模式：自动通过全部人工审核开关（默认开启，方便演示）
 * <p>
 * 开启后（管理端「演示自动审核」按钮）：
 * - 保函：代房东确认、AI复审转人工、人工复审 全部自动通过，直达待缴费/开立
 * - 索赔：AI初审存疑、抗辩后人工裁决 自动通过并模拟赔付
 * - 商户审核、受托支付复核 自动通过
 * <p>
 * 仅影响演示流程推进，不改变任何业务规则定义；关闭后恢复人工审核。
 */
@Service
public class DemoAutoApproveService {

    /** 默认开启（演示优先），管理端可随时切换 */
    private volatile boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
