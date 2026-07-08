package com.stat.web.controller;

import lombok.extern.slf4j.Slf4j;
import com.stat.common.dto.UserDTO;
import com.stat.common.entity.User;
import com.stat.common.result.CommonResult;
import com.stat.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public CommonResult<User> register(@Valid @RequestBody UserDTO userDTO) {
        log.info("收到注册请求，用户名: {}", userDTO.getUsername());
        try {
            User user = userService.register(userDTO);
            return CommonResult.success(user);
        } catch (Exception e) {
            log.error("注册处理异常", e);
            return CommonResult.fail("500", e.getMessage());
        }
    }

    @PostMapping("/login")
    public CommonResult<User> login(@Valid @RequestBody UserDTO userDTO) {
        log.info("收到登录请求，用户名: {}", userDTO.getUsername());
        try {
            User user = userService.login(userDTO.getUsername(), userDTO.getPassword());
            return CommonResult.success(user);
        } catch (Exception e) {
            log.error("登录处理异常", e);
            return CommonResult.fail("500", e.getMessage());
        }
    }

    @PostMapping("/password")
    public CommonResult<Void> changePassword(@RequestBody java.util.Map<String, String> request) {
        String username = com.stat.common.security.UserContext.getUsername();
        if (username == null) {
            return CommonResult.fail("401", "用户未认证");
        }
        String oldPassword = request.get("oldPassword");
        String newPassword = request.get("newPassword");
        if (oldPassword == null || newPassword == null) {
            return CommonResult.fail("400", "旧密码和新密码不能为空");
        }
        try {
            userService.changePassword(username, oldPassword, newPassword);
            return CommonResult.success("密码修改成功");
        } catch (Exception e) {
            return CommonResult.fail("400", "密码修改失败: " + e.getMessage());
        }
    }
} 