package com.weather.util;

import com.weather.entity.User;
import com.weather.mapper.UserMapper;
import org.springframework.stereotype.Component;

/**
 * 管理员权限校验工具
 */
@Component
public class AdminUtils {

    private final UserMapper userMapper;

    public AdminUtils(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 校验指定用户是否为管理员
     *
     * @param userId 当前登录用户 ID
     * @throws IllegalArgumentException 如果用户不存在或非管理员
     */
    public void checkAdmin(Long userId) {
        User user = userMapper.findById(Math.toIntExact(userId));
        if (user == null || user.getPermission() == null || user.getPermission() != 2) {
            throw new IllegalArgumentException("无权限访问");
        }
    }
}
