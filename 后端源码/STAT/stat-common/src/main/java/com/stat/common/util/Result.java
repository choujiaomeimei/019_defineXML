package com.stat.common.util;

import lombok.Data;

/**
 * 统一API响应结果类
 * @param <T> 数据类型
 */
@Data
public class Result<T> {
    
    private String code;
    private String message;
    private T data;
    private boolean success;
    
    public Result() {}
    
    public Result(String code, String message, T data, boolean success) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.success = success;
    }
    
    /**
     * 成功响应
     */
    public static <T> Result<T> success(T data) {
        return new Result<>("200", "操作成功", data, true);
    }
    
    /**
     * 成功响应（无数据）
     */
    public static <T> Result<T> success(T data, String message) {
        return new Result<>("200", message, data, true);
    }
    
    /**
     * 成功响应（无数据）
     */
    public static <T> Result<T> success() {
        return new Result<>("200", "操作成功", null, true);
    }
    
    /**
     * 失败响应
     */
    public static <T> Result<T> error(String message) {
        return new Result<>("500", message, null, false);
    }
    
    /**
     * 失败响应（自定义错误码）
     */
    public static <T> Result<T> error(String code, String message) {
        return new Result<>(code, message, null, false);
    }
} 