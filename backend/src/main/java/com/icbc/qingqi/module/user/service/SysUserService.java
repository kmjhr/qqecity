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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final RegistrationReviewService reviewService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${jwt.access-token-ttl}")
    private Long accessTokenTtl;

    public SysUserService(SysUserMapper userMapper,
                          JwtUtil jwtUtil,
                          RegistrationReviewService reviewService) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
        this.reviewService = reviewService;
    }

    // ============================================================
    //  认证相关
    // ============================================================

    /**
     * 用户注册（默认注册为普通用户 USER）
     * <p>
     * 流程：AI 智能审核（模拟）→ 通过后落库 → 审核记录留痕 → 发送欢迎站内信。
     * AI 审核规则见 {@link RegistrationReviewService}：
     * ① 白名单人群（在校生/毕业2年内/青年创业者，STUDENT/GRADUATE 需学历核验）
     * ② 同一材料/同一人（身份证号、手机号查重）
     * ③ 重复注册（用户名/证件号/手机号三重查重）
     */
    @Transactional(rollbackFor = Exception.class)
    public RegisterReviewVO register(RegisterDTO dto) {
        // 1. AI 智能审核（模拟）：不通过时抛 2002（重复注册）/3001（非白名单等）
        RegisterReviewVO review = reviewService.reviewOrThrow(dto);

        // 2. 创建用户（实名/学历信息一并入库）
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setUserType(dto.getUserType());
        user.setRealName(dto.getRealName());
        user.setIdCard(dto.getIdCard());
        user.setSchool(dto.getSchool());
        user.setGraduationDate(dto.getGraduationDate());
        user.setRole("USER");
        user.setStatus(1);
        userMapper.insert(user);

        // 3. 审核记录落库（留痕）+ 欢迎站内信
        review.setUserId(user.getId());
        reviewService.persist(dto, review, user.getId());
        reviewService.sendWelcomeMessage(user.getId());
        return review;
    }

    /**
     * 用户登录，返回 Token 对
     */
    public TokenVO login(LoginDTO dto) {
        // 查找用户
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername()));
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }

        // 校验密码
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "密码错误");
        }

        // 校验状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "账号已被禁用");
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
            throw new BizException(ErrorCode.USER_NOT_FOUND);
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
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "原密码错误");
        }
        SysUser update = new SysUser();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(newPassword));
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
            throw new BizException(ErrorCode.USER_ALREADY_EXISTS);
        }
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
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
            throw new BizException(ErrorCode.USER_NOT_FOUND);
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
