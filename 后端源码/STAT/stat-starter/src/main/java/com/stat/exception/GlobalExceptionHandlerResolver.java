package com.stat.exception;

import com.stat.common.exception.BusinessException;
import com.stat.common.result.CommonResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.io.IOException;
import java.util.Set;


@Slf4j
@RestControllerAdvice
@SuppressWarnings("rawtypes")
public class GlobalExceptionHandlerResolver {

    /**
     * 处理业务异常
     *
     * @param req req
     * @param e   BusinessException
     * @return JsonResult
     */
    @ExceptionHandler(value = BusinessException.class)
    @ResponseBody
    public CommonResult businessExceptionHandler(HttpServletRequest req, BusinessException e) {
        log.error("发生业务处理异常！原因是: ", e);
        return CommonResult.fail(e.getCode(),e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public CommonResult handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        StringBuilder errorInfo = new StringBuilder();
        BindingResult bindingResult = e.getBindingResult();
        for(int i = 0; i < bindingResult.getFieldErrors().size(); i++){
            if(i > 0){
                errorInfo.append(",");
            }
            FieldError fieldError = bindingResult.getFieldErrors().get(i);
            errorInfo.append(fieldError.getField()).append(" :").append(fieldError.getDefaultMessage());
        }

        return CommonResult.fail("400", errorInfo.toString());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public CommonResult<String> handleConstraintViolationException(ConstraintViolationException e) {
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        if (violations == null || violations.isEmpty()) {
            return CommonResult.fail("400", e.getMessage());
        }
        String errorMessage = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(java.util.stream.Collectors.joining(","));
        return CommonResult.fail("400", errorMessage);
    }

    /**
     * 处理HttpMessageNotWritableException异常
     * 通常发生在响应Content-Type不匹配时
     */
    @ExceptionHandler(HttpMessageNotWritableException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public CommonResult<String> handleHttpMessageNotWritableException(HttpMessageNotWritableException e, 
                                                                     HttpServletRequest request,
                                                                     HttpServletResponse response) {
        log.error("HttpMessageNotWritableException: {}", e.getMessage());
        
        // 检查响应是否已经提交
        if (response.isCommitted()) {
            log.warn("Response has already been committed, cannot return JSON error response");
            return null; // 返回null，让Spring处理
        }
        
        return CommonResult.fail("500", "响应格式错误: " + e.getMessage());
    }

    /**
     * 处理IllegalStateException异常（响应已提交）
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public CommonResult<String> handleIllegalStateException(IllegalStateException e, 
                                                           HttpServletRequest request,
                                                           HttpServletResponse response) {
        log.error("IllegalStateException: {}", e.getMessage());
        
        // 检查是否是响应已提交的错误
        if (e.getMessage() != null && e.getMessage().contains("Response has already been committed")) {
            log.warn("Response has already been committed, cannot return JSON error response");
            return null; // 返回null，让Spring处理
        }
        
        return CommonResult.fail("500", "服务器状态错误: " + e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public CommonResult<String> handleDefaultException(Exception e, 
                                                      HttpServletRequest request,
                                                      HttpServletResponse response) {
        log.error("发生未知异常: ", e);
        
        // 检查响应是否已经提交
        if (response.isCommitted()) {
            log.warn("Response has already been committed, cannot return JSON error response");
            return null; // 返回null，让Spring处理
        }
        
        return CommonResult.fail("500", "服务器内部错误: " + e.getMessage());
    }
}
