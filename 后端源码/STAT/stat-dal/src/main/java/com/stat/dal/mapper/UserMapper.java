package com.stat.dal.mapper;

import com.stat.common.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface UserMapper {
    
    @Select("SELECT * FROM user WHERE username = #{username}")
    User selectByUsername(String username);
    
    @Select("SELECT COUNT(*) FROM user WHERE username = #{username}")
    int checkUsernameExists(String username);
    
    @Insert("INSERT INTO user (username, password, email, create_time, update_time) " +
           "VALUES (#{username}, #{password}, #{email}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    int updateUser(User user);
} 