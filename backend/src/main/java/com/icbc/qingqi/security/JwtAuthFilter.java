package com.icbc.qingqi.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 鉴权过滤器：解析 Authorization: Bearer <token>，写入登录上下文。
 * 骨架阶段只负责识别登录态，具体接口按需通过 UserContext 校验。
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HEADER);
        try {
            if (header != null && header.startsWith(PREFIX)) {
                Claims claims = jwtUtil.parse(header.substring(PREFIX.length()));
                Long userId = jwtUtil.getUserId(claims);
                String name = claims.get("name", String.class);
                UserContext.set(userId, name);
            }
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }
}
