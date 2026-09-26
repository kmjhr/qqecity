package com.icbc.qingqi.module.user;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.icbc.qingqi.common.BizException;
import com.icbc.qingqi.common.ErrorCode;
import com.icbc.qingqi.security.PasswordUtil;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户与授权中心：注册、登录、当前用户
 */
@Service
public class UserService {

    private final UserMapper userMapper;
    private final com.icbc.qingqi.security.JwtUtil jwtUtil;

    public UserService(UserMapper userMapper, com.icbc.qingqi.security.JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }

    public Long register(User user, String rawPassword) {
        Long count = userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getPhone, user.getPhone()));
        if (count != null && count > 0) {
            throw new BizException(3001, "该手机号已注册");
        }
        user.setPasswordHash(PasswordUtil.hash(rawPassword));
        user.setStatus(0);
        userMapper.insert(user);
        return user.getUserId();
    }

    public Map<String, Object> login(String phone, String rawPassword) {
        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getPhone, phone));
        if (user == null || !PasswordUtil.matches(rawPassword, user.getPasswordHash())) {
            throw new BizException(3001, "手机号或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 1) {
            throw new BizException(3001, "账户已冻结");
        }
        String token = jwtUtil.generate(user.getUserId(), user.getName());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getUserId());
        data.put("name", user.getName());
        data.put("userType", user.getUserType());
        return data;
    }

    public User getById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        return user;
    }
}
