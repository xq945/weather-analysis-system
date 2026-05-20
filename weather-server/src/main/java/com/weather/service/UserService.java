package com.weather.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.weather.entity.User;
import com.weather.mapper.UserMapper;
import com.weather.util.AdminUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final AdminUtils adminUtils;

    public UserService(UserMapper userMapper, AdminUtils adminUtils) {
        this.userMapper = userMapper;
        this.adminUtils = adminUtils;
    }

    public List<User> listUsers(Long adminUserId) {
        adminUtils.checkAdmin(adminUserId);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(User::getId);
        List<User> users = userMapper.selectList(wrapper);
        users.forEach(u -> u.setPassword(null));
        return users;
    }

    public User updateStatus(Long adminUserId, Integer userId, Integer status) {
        adminUtils.checkAdmin(adminUserId);
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
        adminUtils.checkAdmin(adminUserId);
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
