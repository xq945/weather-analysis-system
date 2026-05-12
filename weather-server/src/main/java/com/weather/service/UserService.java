package com.weather.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.weather.entity.User;
import com.weather.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    private void checkAdmin(Long userId) {
        User admin = userMapper.selectById(userId);
        if (admin == null || admin.getPermission() == null || admin.getPermission() != 2) {
            throw new IllegalArgumentException("无权限访问");
        }
    }

    public List<User> listUsers(Long adminUserId) {
        checkAdmin(adminUserId);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(User::getId);
        List<User> users = userMapper.selectList(wrapper);
        users.forEach(u -> u.setPassword(null));
        return users;
    }

    public User updateStatus(Long adminUserId, Integer userId, Integer status) {
        checkAdmin(adminUserId);
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        user.setStatus(status);
        userMapper.updateById(user);
        user.setPassword(null);
        return user;
    }

    public User updatePermission(Long adminUserId, Integer userId, Integer permission) {
        checkAdmin(adminUserId);
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        user.setPermission(permission);
        userMapper.updateById(user);
        user.setPassword(null);
        return user;
    }
}
