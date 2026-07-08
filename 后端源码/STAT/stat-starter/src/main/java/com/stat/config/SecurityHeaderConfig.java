package com.stat.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
public class SecurityHeaderConfig implements WebMvcConfigurer {

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilter() {
        FilterRegistrationBean<JwtAuthenticationFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new JwtAuthenticationFilter());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(0);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<SecurityHeaderFilter> securityHeaderFilter() {
        FilterRegistrationBean<SecurityHeaderFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new SecurityHeaderFilter());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }

    public static class SecurityHeaderFilter implements Filter {

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            
            // 设置内容安全策略，允许blob URL
            httpResponse.setHeader("Content-Security-Policy", 
                "default-src 'self'; " +
                "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                "style-src 'self' 'unsafe-inline'; " +
                "img-src 'self' data: blob:; " +
                "media-src 'self' blob:; " +
                "object-src 'none'; " +
                "frame-src 'none'; " +
                "connect-src 'self' blob:; " +
                "font-src 'self' data:; " +
                "manifest-src 'self'; " +
                "worker-src 'self' blob:;"
            );
            
            // 设置X-Content-Type-Options
            httpResponse.setHeader("X-Content-Type-Options", "nosniff");
            
            // 设置X-Frame-Options
            httpResponse.setHeader("X-Frame-Options", "DENY");
            
            // 设置X-XSS-Protection
            httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
            
            // 设置Referrer-Policy
            httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
            
            // 对于导出接口，设置特殊的头部
            if (httpRequest.getRequestURI().startsWith("/export/")) {
                httpResponse.setHeader("Content-Type", "application/octet-stream");
                httpResponse.setHeader("Access-Control-Allow-Origin", "*");
                httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
                httpResponse.setHeader("Access-Control-Allow-Headers", "Origin, Content-Type, Accept, Authorization, X-Requested-With");
                httpResponse.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
            }
            
            chain.doFilter(request, response);
        }

        @Override
        public void init(FilterConfig filterConfig) throws ServletException {
            // 初始化过滤器
        }

        @Override
        public void destroy() {
            // 清理资源
        }
    }
} 