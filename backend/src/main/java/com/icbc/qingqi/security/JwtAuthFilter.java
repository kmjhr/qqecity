package com.icbc.qingqi.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icbc.qingqi.common.ErrorCode;
import com.icbc.qingqi.common.Result;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 鉴权过滤器
 * <p>
 * 工作流程：
 * 1. 从请求头 Authorization 中提取 Token
 * 2. 校验 Token 有效性（签名、过期、黑名单）
 * 3. 将用户信息放入 UserContext（ThreadLocal）
 * 4. 放行 / 拦截
 * <p>
 * 白名单：登录、注册、swagger 文档等接口直接放行
 * 管理端接口（/admin/**）要求 ADMIN 或 BANK_OPERATOR 角色
 * 用户端接口（/v1/**）要求已登录
 * <p>
 * 错误码对齐文档定义：
 * - 未登录/Token 无效：1002
 * - 越权访问（非后台角色访问管理端）：4001
 */
@Slf4j
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Value("${jwt.header}")
    private String header;

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    public JwtAuthFilter(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 白名单路径直接放行
        if (isWhitelist(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 获取 Token
        String authHeader = request.getHeader(header);
        String token = jwtUtil.extractToken(authHeader);

        if (token == null || token.isEmpty()) {
            writeError(response, ErrorCode.UNAUTHORIZED);
            return;
        }

        // 校验 Token
        String error = jwtUtil.validateToken(token);
        if (error != null) {
            // Token 无效或过期，统一返回 1002 未登录或登录已过期
            writeError(response, ErrorCode.UNAUTHORIZED);
            return;
        }

        // 解析用户信息，放入上下文
        Long userId = jwtUtil.getUserId(token);
        String username = jwtUtil.getUsername(token);
        String role = jwtUtil.getRole(token);

        // 管理端接口需要 ADMIN 或 banker 角色，越权返回 4001
        if (isAdminPath(uri) && !isBackOffice(role)) {
            writeError(response, ErrorCode.FORBIDDEN);
            return;
        }

        UserContext.set(new UserContext.CurrentUser(userId, username, role));

        try {
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }

    /**
     * 判断是否为白名单路径（无需登录）
     */
    private boolean isWhitelist(String uri) {
        // 认证相关
        if (uri.contains("/auth/login")
                || uri.contains("/auth/register")
                || uri.contains("/auth/refresh")) {
            return true;
        }
        // 接口文档
        if (uri.contains("/v3/api-docs")
                || uri.contains("/swagger-ui")
                || uri.contains("/doc.html")
                || uri.contains("/webjars")
                || uri.contains("/swagger-resources")
                || uri.contains("/favicon.ico")) {
            return true;
        }
        // 健康检查
        return uri.contains("/health") || uri.equals("/api/");
    }

    /**
     * 判断是否为管理端接口
     */
    private boolean isAdminPath(String uri) {
        return uri.contains("/admin/");
    }

    /**
     * 判断是否为后台角色（ADMIN 系统管理员 / BANK_OPERATOR 银行运营岗）
     * 管理端审核台允许两类角色进入（补充计划书 §8）
     */
    private boolean isBackOffice(String role) {
        return "ADMIN".equals(role) || "BANK_OPERATOR".equals(role);
    }

    /**
     * 写出错误响应
     */
    private void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(errorCode)));
    }
}
