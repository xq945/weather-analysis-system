package com.weather.util;

import com.weather.entity.User;
import com.weather.mapper.UserMapper;
import org.springframework.stereotype.Component;

@Component
public class AdminUtils {

    private final UserMapper userMapper;

    public AdminUtils(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public void checkAdmin(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getPermission() == null || user.getPermission() != 2) {
            throw new IllegalArgumentException("无权限访问");
        }
    }
}
