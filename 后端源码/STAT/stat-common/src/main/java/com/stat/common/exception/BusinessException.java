package com.stat.common.exception;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
public class BusinessException extends RuntimeException {
    /**
     * 异常状态码
     */
    private String code;
    /**
     * 异常信息
     */
    private String message;

    public BusinessException(String message) {
        this.code = "500";
        this.message = message;
    }

    public BusinessException(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
