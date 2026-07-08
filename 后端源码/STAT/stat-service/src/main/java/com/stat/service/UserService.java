package com.stat.service;

import com.stat.common.dto.UserDTO;
import com.stat.common.entity.User;

public interface UserService {
    User register(UserDTO userDTO);
    User login(String username, String password);
    void changePassword(String username, String oldPassword, String newPassword);
} 