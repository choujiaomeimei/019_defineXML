package com.stat.config;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stat.common.result.CommonResult;
import com.stat.common.security.UserContext;
import com.stat.common.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class JwtAuthenticationFilter implements Filter {

    private static final List<String> WHITE_LIST = Arrays.asList(
            "/user/login",
            "/user/register",
            "/swagger-ui",
            "/v3/api-docs",
            "/actuator/health",
            "/actuator/info"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();

        // OPTIONS preflight requests pass through
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        // White-listed paths pass through
        if (isWhiteListed(path)) {
            chain.doFilter(request, response);
            return;
        }

        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorized(httpResponse, "缺少认证令牌");
            return;
        }

        String token = authHeader.substring(7);
        DecodedJWT jwt = JwtUtil.verifyToken(token);
        if (jwt == null) {
            sendUnauthorized(httpResponse, "认证令牌无效或已过期");
            return;
        }

        String username = jwt.getClaim("username").asString();
        Long userId = jwt.getClaim("userId").asLong();
        UserContext.set(new UserContext.UserInfo(userId, username));

        try {
            chain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }

    private boolean isWhiteListed(String path) {
        return WHITE_LIST.stream().anyMatch(path::startsWith);
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        CommonResult<?> result = CommonResult.fail("401", message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void destroy() {}
}
