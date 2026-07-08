package com.stat.common.result;

import com.stat.common.base.RdfaObject;

import java.io.Serializable;

/**
 * @description:
 * @author: yujq
 **/
public class CommonResult<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    private boolean success;
    private String code;
    private String message;
    private T data;

    public CommonResult() {
        this.success = true;
    }

    public CommonResult(String code, String message) {
        this(true, code, message);
    }

    public CommonResult(boolean success, String code, String message) {
        this(success, code, message, null, (Throwable)null);
    }

    public CommonResult(boolean success, String code, String message, T data) {
        this(success, code, message, data, (Throwable)null);
    }

    public CommonResult(boolean success, String code, String message, T data, Throwable cause) {
        this.success = true;
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // public static <T extends Serializable> CommonResult<T> success(String code, String message, T data) {
    //     CommonResult rest = new CommonResult();
    //     rest.setCode(code);
    //     rest.setMessage(message);
    //     rest.setData(data);
    //     rest.setSuccess(true);
    //     return rest;
    // }

    // public static <T extends Serializable> CommonResult<T> success(T data) {
    //     CommonResult rest = new CommonResult();
    //     rest.setCode("0");
    //     rest.setMessage("");
    //     rest.setData(data);
    //     rest.setSuccess(true);
    //     return rest;
    // }
    // public static CommonResult success() {
    //     CommonResult rest = new CommonResult();
    //     rest.setCode("0");
    //     rest.setMessage("");
    //     rest.setSuccess(true);
    //     return rest;
    // }

    // public static <T extends Serializable> CommonResult<T> fail(String code, String message) {
    //     CommonResult<T> rest = new CommonResult();
    //     rest.setCode(code);
    //     rest.setMessage(message);
    //     rest.setSuccess(false);
    //     return rest;
    // }

    public static <T> CommonResult<T> success(String code, String message, T data) {
        CommonResult<T> rest = new CommonResult<>();
        rest.setCode(code);
        rest.setMessage(message);
        rest.setData(data);
        rest.setSuccess(true);
        return rest;
    }
    
    public static <T> CommonResult<T> success(T data) {
        CommonResult<T> rest = new CommonResult<>();
        rest.setCode("0");
        rest.setMessage("");
        rest.setData(data);
        rest.setSuccess(true);
        return rest;
    }
    
    public static <T> CommonResult<T> success() {
        CommonResult<T> rest = new CommonResult<>();
        rest.setCode("0");
        rest.setMessage("");
        rest.setSuccess(true);
        return rest;
    }
    
    public static <T> CommonResult<T> fail(String code, String message) {
        CommonResult<T> rest = new CommonResult<>();
        rest.setCode(code);
        rest.setMessage(message);
        rest.setSuccess(false);
        return rest;
    }
    
    public static <T> CommonResult<T> failed(String message) {
        CommonResult<T> rest = new CommonResult<>();
        rest.setCode("500");
        rest.setMessage(message);
        rest.setSuccess(false);
        return rest;
    }
    
    public static <T> CommonResult<T> success(String message) {
        CommonResult<T> rest = new CommonResult<>();
        rest.setCode("200");
        rest.setMessage(message);
        rest.setSuccess(true);
        return rest;
    }



    public String toLog() {
        int length = 20;
        String data_str = null;
        if (this.code != null) {
            length += this.code.length();
        }

        if (this.message != null) {
            length += this.message.length();
        }

        if (this.data != null) {
            if (this.data instanceof RdfaObject) {
                data_str = ((RdfaObject)this.data).toLog();
                length += data_str.length();
            } else {
                data_str = this.data.toString();
                length += data_str.length();
            }
        }

        StringBuilder sb = new StringBuilder(length);
        sb.append("code:");
        sb.append(this.code);
        sb.append(" message:");
        sb.append(this.message);
        sb.append(" data:");
        sb.append(data_str);
        return sb.toString();
    }

    public boolean isSuccess() {
        return this.success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return this.data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
