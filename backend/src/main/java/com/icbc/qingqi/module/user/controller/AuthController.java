package com.icbc.qingqi.module.user.controller;

import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.module.user.dto.LoginDTO;
import com.icbc.qingqi.module.user.dto.RegisterDTO;
import com.icbc.qingqi.module.user.dto.TokenVO;
import com.icbc.qingqi.module.user.service.SysUserService;
import com.icbc.qingqi.security.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口
 * <p>
 * 用户端和管理端共用一套登录接口，通过角色区分权限
 * 接口路径：/api/v1/auth/** （白名单，不需要登录）
 * <p>
 * 原 auth/controller/AuthController.java，现合并到 user 模块下
 */
@Tag(name = "认证接口")
@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private final SysUserService userService;
    private final JwtUtil jwtUtil;

    @Value("${jwt.header}")
    private String header;

    public AuthController(SysUserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        userService.register(dto);
        return Result.success();
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<TokenVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(userService.login(dto));
    }

    @Operation(summary = "刷新 Token")
    @PostMapping("/refresh")
    public Result<TokenVO> refreshToken(@RequestBody RefreshRequest req) {
        return Result.success(userService.refreshToken(req.getRefreshToken()));
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String authHeader = request.getHeader(header);
        String token = jwtUtil.extractToken(authHeader);
        if (token != null) {
            userService.logout(token);
        }
        return Result.success();
    }

    /**
     * 内部类：刷新令牌请求体
     */
    @Data
    public static class RefreshRequest {
        private String refreshToken;
    }
}
