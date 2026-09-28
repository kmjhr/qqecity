package com.icbc.qingqi.module.user.controller;

import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.module.user.dto.*;
import com.icbc.qingqi.module.user.service.RegistrationReviewService;
import com.icbc.qingqi.module.user.service.SysUserService;
import com.icbc.qingqi.security.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 认证接口
 * <p>
 * 用户端和管理端共用一套登录接口，通过角色区分权限
 * 接口路径：/api/v1/auth/** （白名单，不需要登录）
 * <p>
 * 原 auth/controller/AuthController.java，现合并到 user 模块下
 * <p>
 * 注册流程增强（模拟 AI 审核）：
 * - POST /register/ai-review   注册 AI 预审（不落库、不建号）
 * - POST /register             正式注册（先过 AI 审核，通过才落库 + 审核留痕 + 欢迎站内信）
 * - GET  /schools              高校库列表（注册页学校下拉）
 * - POST /student-card/ocr     学生证/毕业证照片 AI 识别（模拟，按核验方式通用）
 */
@Tag(name = "认证接口")
@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private final SysUserService userService;
    private final RegistrationReviewService reviewService;
    private final JwtUtil jwtUtil;

    @Value("${jwt.header}")
    private String header;

    public AuthController(SysUserService userService,
                          RegistrationReviewService reviewService,
                          JwtUtil jwtUtil) {
        this.userService = userService;
        this.reviewService = reviewService;
        this.jwtUtil = jwtUtil;
    }

    @Operation(summary = "用户注册（AI 智能审核，模拟）")
    @PostMapping("/register")
    public Result<RegisterReviewVO> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.success(userService.register(dto));
    }

    @Operation(summary = "注册 AI 预审（模拟，不落库不建号）")
    @PostMapping("/register/ai-review")
    public Result<RegisterReviewVO> aiReview(@RequestBody RegisterDTO dto) {
        return Result.success(reviewService.review(dto));
    }

    @Operation(summary = "高校库列表（注册学历核验白名单，模拟）")
    @GetMapping("/schools")
    public Result<List<SchoolVO>> schools() {
        return Result.success(reviewService.listSchools());
    }

    @Operation(summary = "学生证/毕业证照片 AI 识别（模拟）")
    @PostMapping("/student-card/ocr")
    public Result<StudentCardOcrVO> studentCardOcr(@RequestBody(required = false) StudentCardOcrDTO dto) {
        return Result.success(reviewService.mockOcr(dto != null ? dto : new StudentCardOcrDTO()));
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
