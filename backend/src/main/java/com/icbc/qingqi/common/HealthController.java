package com.icbc.qingqi.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查接口
 * <p>
 * 用于部署文档/运维脚本验证后端是否存活（JwtAuthFilter 白名单已放行 /health）。
 * 返回：{"code":0,"message":"success","data":"ok"}
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping
    public Result<String> health() {
        return Result.success("ok");
    }
}
