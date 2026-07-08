package com.stat.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for controller/service methods that require the current user
 * to be a member of the specified project. The projectId is resolved from
 * the method parameter named "projectId" by default.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireProjectAccess {
    String value() default "projectId";
    String[] roles() default {};
}
