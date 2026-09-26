package com.icbc.qingqi.module.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.module.user.dto.UserCreateDTO;
import com.icbc.qingqi.module.user.dto.UserUpdateDTO;
import com.icbc.qingqi.module.user.dto.UserVO;
import com.icbc.qingqi.module.user.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端 - 用户管理接口
 * <p>
 * 路径：/api/admin/v1/user/** （需要 ADMIN 角色）
 * 供管理员后台网页调用
 * <p>
 * 原 system/user/controller/AdminUserController.java，现移至 module/user 下
 */
@Tag(name = "管理端 - 用户管理")
@RestController
@RequestMapping("/admin/v1/user")
public class AdminUserController {

    private final SysUserService userService;

    public AdminUserController(SysUserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "分页查询用户列表")
    @GetMapping("/page")
    public Result<Page<UserVO>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        return Result.success(userService.pageUsers(pageNum, pageSize, keyword));
    }

    @Operation(summary = "获取用户详情")
    @GetMapping("/{id}")
    public Result<UserVO> getById(@PathVariable Long id) {
        return Result.success(userService.getUserById(id));
    }

    @Operation(summary = "新增用户")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody UserCreateDTO dto) {
        userService.createUser(dto);
        return Result.success();
    }

    @Operation(summary = "修改用户")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody UserUpdateDTO dto) {
        userService.updateUser(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }
}
