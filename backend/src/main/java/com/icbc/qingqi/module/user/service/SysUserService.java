package com.icbc.qingqi.module.user.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.icbc.qingqi.common.BizException;
import com.icbc.qingqi.common.ErrorCode;
import com.icbc.qingqi.module.user.dto.*;
import com.icbc.qingqi.module.user.entity.SysUser;
import com.icbc.qingqi.module.user.mapper.SysUserMapper;
import com.icbc.qingqi.security.JwtUtil;
import com.icbc.qingqi.security.PasswordUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 用户服务
 * <p>
 * 提供用户注册、登录、信息查询/修改、用户管理（管理员）等能力
 * 密码加密：BCrypt（Spring Security 的 BCryptPasswordEncoder，
 *           只引入加密器，不引入完整 Spring Security）
 */
@Service
public class SysUserService {

    private final SysUserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordUtil passwordUtil;

    @Value("${jwt.access-token-ttl}")
    private Long accessTokenTtl;

    public SysUserService(SysUserMapper userMapper, JwtUtil jwtUtil, PasswordUtil passwordUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
        this.passwordUtil = passwordUtil;
    }

    // ============================================================
    //  认证相关
    // ============================================================

    /**
     * 用户注册（默认注册为普通用户 USER）
     * <p>
     * 保存 userType 人群类型字段
     */
    public void register(RegisterDTO dto) {
        // 检查用户名是否已存在
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername()));
        if (count != null && count > 0) {
            throw new BizException(ErrorCode.CONFLICT, "用户已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordUtil.encode(dto.getPassword()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setUserType(dto.getUserType());
        user.setRole("USER");
        user.setStatus(1);
        userMapper.insert(user);
    }

    /**
     * 用户登录，返回 Token 对
     */
    public TokenVO login(LoginDTO dto) {
        // 查找用户
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername()));
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        // 校验密码
        if (!passwordUtil.matches(dto.getPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.BIZ_ERROR, "密码错误");
        }

        // 校验状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException(ErrorCode.BIZ_ERROR, "账号已被禁用");
        }

        // 生成 Token
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername(), user.getRole());

        TokenVO vo = new TokenVO();
        vo.setAccessToken(accessToken);
        vo.setRefreshToken(refreshToken);
        vo.setExpiresIn(accessTokenTtl);
        vo.setUser(toVO(user));
        return vo;
    }

    /**
     * 刷新 Token
     */
    public TokenVO refreshToken(String refreshToken) {
        // 解析刷新令牌
        Long userId;
        String username;
        String role;
        try {
            userId = jwtUtil.getUserId(refreshToken);
            username = jwtUtil.getUsername(refreshToken);
            role = jwtUtil.getRole(refreshToken);
        } catch (Exception e) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "刷新令牌无效");
        }

        // 生成新的访问令牌
        String newAccessToken = jwtUtil.generateAccessToken(userId, username, role);
        String newRefreshToken = jwtUtil.generateRefreshToken(userId, username, role);

        TokenVO vo = new TokenVO();
        vo.setAccessToken(newAccessToken);
        vo.setRefreshToken(newRefreshToken);
        vo.setExpiresIn(accessTokenTtl);
        return vo;
    }

    /**
     * 登出（将 accessToken 加入黑名单）
     */
    public void logout(String token) {
        jwtUtil.invalidateToken(token);
    }

    // ============================================================
    //  用户信息
    // ============================================================

    /**
     * 获取当前用户信息
     */
    public UserVO getCurrentUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return toVO(user);
    }

    /**
     * 修改当前用户信息
     */
    public void updateCurrentUser(Long userId, UserUpdateDTO dto) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setNickname(dto.getNickname());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setAvatar(dto.getAvatar());
        user.setUserType(dto.getUserType());
        userMapper.updateById(user);
    }

    /**
     * 修改密码
     */
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (!passwordUtil.matches(oldPassword, user.getPassword())) {
            throw new BizException(ErrorCode.BIZ_ERROR, "原密码错误");
        }
        SysUser update = new SysUser();
        update.setId(userId);
        update.setPassword(passwordUtil.encode(newPassword));
        userMapper.updateById(update);
    }

    // ============================================================
    //  管理员 - 用户管理
    // ============================================================

    /**
     * 分页查询用户列表
     */
    public Page<UserVO> pageUsers(int pageNum, int pageSize, String keyword) {
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getNickname, keyword)
                    .or().like(SysUser::getPhone, keyword);
        }
        wrapper.orderByDesc(SysUser::getCreateTime);
        Page<SysUser> result = userMapper.selectPage(page, wrapper);

        // 转换为 VO
        Page<UserVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    /**
     * 管理员新增用户
     */
    public void createUser(UserCreateDTO dto) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername()));
        if (count != null && count > 0) {
            throw new BizException(ErrorCode.CONFLICT, "用户已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordUtil.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setUserType(dto.getUserType());
        user.setRole(dto.getRole() != null ? dto.getRole() : "USER");
        user.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        userMapper.insert(user);
    }

    /**
     * 管理员修改用户
     */
    public void updateUser(Long id, UserUpdateDTO dto) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setNickname(dto.getNickname());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setAvatar(dto.getAvatar());
        user.setUserType(dto.getUserType());
        user.setRole(dto.getRole());
        user.setStatus(dto.getStatus());
        userMapper.updateById(user);
    }

    /**
     * 管理员删除用户（逻辑删除）
     */
    public void deleteUser(Long id) {
        userMapper.deleteById(id);
    }

    /**
     * 管理员获取用户详情
     */
    public UserVO getUserById(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return toVO(user);
    }

    // ============================================================
    //  工具方法
    // ============================================================

    private UserVO toVO(SysUser user) {
        UserVO vo = new UserVO();
        BeanUtil.copyProperties(user, vo);
        return vo;
    }
}
