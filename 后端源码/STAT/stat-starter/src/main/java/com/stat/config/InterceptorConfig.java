package com.stat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {
    // SpringDoc OpenAPI handles its own static resources via /swagger-ui/**
}
