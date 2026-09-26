package com.icbc.qingqi.module.user.controller;

import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.module.user.dto.UserUpdateDTO;
import com.icbc.qingqi.module.user.dto.UserVO;
import com.icbc.qingqi.module.user.service.SysUserService;
import com.icbc.qingqi.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

/**
 * 用户端 - 个人信息接口
 * <p>
 * 路径：/api/v1/user/** （需要登录，USER 或 ADMIN 角色均可访问）
 * 供用户端网页、微信小程序调用
 * <p>
 * 原 system/user/controller/UserController.java，现移至 module/user 下
 */
@Tag(name = "用户端 - 个人信息")
@RestController
@RequestMapping("/v1/user")
public class UserController {

    private final SysUserService userService;

    public UserController(SysUserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/profile")
    public Result<UserVO> getProfile() {
        Long userId = UserContext.getUserId();
        return Result.success(userService.getCurrentUser(userId));
    }

    @Operation(summary = "修改个人信息")
    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody UserUpdateDTO dto) {
        Long userId = UserContext.getUserId();
        // 防止用户越权修改角色和状态
        dto.setRole(null);
        dto.setStatus(null);
        userService.updateCurrentUser(userId, dto);
        return Result.success();
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody PasswordChangeRequest req) {
        Long userId = UserContext.getUserId();
        userService.changePassword(userId, req.getOldPassword(), req.getNewPassword());
        return Result.success();
    }

    @Data
    public static class PasswordChangeRequest {
        private String oldPassword;
        private String newPassword;
    }
}
