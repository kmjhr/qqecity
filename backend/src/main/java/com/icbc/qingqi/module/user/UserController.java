package com.icbc.qingqi.module.user;

import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.security.UserContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用户与授权中心接口
 */
@RestController
@RequestMapping("/api/v1/auth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Data
    public static class RegisterRequest {
        @NotBlank(message = "姓名不能为空")
        private String name;
        @NotBlank(message = "手机号不能为空")
        private String phone;
        @NotBlank(message = "证件号码不能为空")
        private String idNumber;
        @NotBlank(message = "人群类型不能为空")
        private String userType;
        @NotBlank(message = "密码不能为空")
        private String password;
        private String school;
        private String eduLevel;
        private String gradYear;
    }

    @Data
    public static class LoginRequest {
        @NotBlank(message = "手机号不能为空")
        private String phone;
        @NotBlank(message = "密码不能为空")
        private String password;
    }

    @PostMapping("/register")
    public Result<Long> register(@Valid @RequestBody RegisterRequest req) {
        User user = new User();
        user.setName(req.getName());
        user.setPhone(req.getPhone());
        user.setIdNumber(req.getIdNumber());
        user.setUserType(req.getUserType());
        user.setSchool(req.getSchool());
        user.setEduLevel(req.getEduLevel());
        user.setGradYear(req.getGradYear());
        return Result.ok(userService.register(user, req.getPassword()));
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest req) {
        return Result.ok(userService.login(req.getPhone(), req.getPassword()));
    }

    @GetMapping("/me")
    public Result<User> me() {
        User user = userService.getById(UserContext.requireUserId());
        user.setPasswordHash(null); // 不返回敏感字段
        return Result.ok(user);
    }
}
