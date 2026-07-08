package com.stat.service.impl;

import com.stat.common.dto.UserDTO;
import com.stat.common.entity.User;
import com.stat.common.exception.BusinessException;
import com.stat.common.util.JwtUtil;
import com.stat.common.util.PasswordUtil;
import com.stat.service.UserService;
import com.stat.dal.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public User register(UserDTO userDTO) {
        log.info("开始注册用户，用户名: {}", userDTO.getUsername());
        try {
            if (userMapper.checkUsernameExists(userDTO.getUsername()) > 0) {
                log.warn("注册失败：用户名 {} 已存在", userDTO.getUsername());
                throw new BusinessException("用户名已存在");
            }

            User user = new User();
            user.setUsername(userDTO.getUsername());
            user.setPassword(PasswordUtil.encode(userDTO.getPassword()));
            user.setEmail(userDTO.getEmail());

            log.info("开始保存用户信息到数据库");
            userMapper.insert(user);
            log.info("用户 {} 注册成功，用户ID: {}", user.getUsername(), user.getId());

            user.setPassword(null);
            return user;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("注册过程发生异常", e);
            throw new BusinessException("注册失败：" + e.getMessage());
        }
    }

    @Override
    public User login(String username, String password) {
        log.info("用户开始登录，用户名: {}", username);
        try {
            User user = userMapper.selectByUsername(username);
            if (user == null || !PasswordUtil.matches(password, user.getPassword())) {
                log.warn("登录失败：用户名或密码错误，用户名: {}", username);
                throw new BusinessException("用户名或密码错误");
            }
            log.info("用户 {} 登录成功", username);
            String token = JwtUtil.generateToken(user.getId(), user.getUsername());
            user.setToken(token);
            user.setPassword(null);
            return user;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("登录过程发生异常", e);
            throw new BusinessException("登录失败：" + e.getMessage());
        }
    }

    @Override
    public void changePassword(String username, String oldPassword, String newPassword) {
        User user = userMapper.selectByUsername(username);
        if (user == null || !PasswordUtil.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("旧密码错误");
        }
        user.setPassword(PasswordUtil.encode(newPassword));
        userMapper.updateUser(user);
        log.info("用户 {} 密码修改成功", username);
    }
} 