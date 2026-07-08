package com.stat.service.aspect;

import com.stat.common.exception.BusinessException;
import com.stat.common.security.RequireProjectAccess;
import com.stat.common.security.UserContext;
import com.stat.dal.mapper.ProjectMemberMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@Slf4j
@Aspect
@Component
public class ProjectAccessAspect {

    @Autowired
    private ProjectMemberMapper projectMemberMapper;

    @Before("@annotation(com.stat.common.security.RequireProjectAccess)")
    public void checkProjectAccess(JoinPoint joinPoint) {
        String username = UserContext.getUsername();
        if (username == null) {
            throw new BusinessException("401", "用户未认证");
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequireProjectAccess annotation = method.getAnnotation(RequireProjectAccess.class);
        String paramName = annotation.value();

        String projectId = resolveProjectId(joinPoint, signature, paramName);
        if (projectId == null || projectId.isEmpty()) {
            return; // no project context, skip check
        }

        int count = projectMemberMapper.checkMembership(projectId, username);
        if (count == 0) {
            log.warn("User {} attempted to access project {} without membership", username, projectId);
            throw new BusinessException("403", "无权访问该项目");
        }

        String[] requiredRoles = annotation.roles();
        if (requiredRoles.length > 0) {
            String role = projectMemberMapper.selectRole(projectId, username);
            boolean hasRole = false;
            for (String r : requiredRoles) {
                if (r.equals(role)) {
                    hasRole = true;
                    break;
                }
            }
            if (!hasRole) {
                throw new BusinessException("403", "无权执行该操作");
            }
        }
    }

    private String resolveProjectId(JoinPoint joinPoint, MethodSignature signature, String paramName) {
        Parameter[] parameters = signature.getMethod().getParameters();
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].getName().equals(paramName) && args[i] != null) {
                return args[i].toString();
            }
        }
        String[] paramNames = signature.getParameterNames();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                if (paramNames[i].equals(paramName) && args[i] != null) {
                    return args[i].toString();
                }
            }
        }
        for (Object arg : args) {
            if (arg == null) continue;
            if (arg instanceof java.util.Map) {
                Object val = ((java.util.Map<?, ?>) arg).get(paramName);
                if (val != null) return val.toString();
            }
            try {
                java.lang.reflect.Field field = arg.getClass().getDeclaredField(paramName);
                field.setAccessible(true);
                Object val = field.get(arg);
                if (val != null) return val.toString();
            } catch (NoSuchFieldException | IllegalAccessException ignored) {
            }
        }
        return null;
    }
}
