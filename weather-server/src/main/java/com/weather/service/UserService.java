package com.weather.service;

import com.weather.entity.User;
import com.weather.mapper.UserMapper;
import com.weather.util.AdminUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户管理服务：列表、修改状态、修改权限
 *
 * 所有接口均需通过 AdminUtils 校验当前用户是否为管理员。
 */
@Service
public class UserService {

    private final UserMapper userMapper;
    private final AdminUtils adminUtils;

    public UserService(UserMapper userMapper, AdminUtils adminUtils) {
        this.userMapper = userMapper;
        this.adminUtils = adminUtils;
    }

    /** 获取所有用户列表（返回前清空密码） */
    public List<User> listUsers(Long adminUserId) {
        adminUtils.checkAdmin(adminUserId);
        List<User> users = userMapper.listAllOrderById();
        users.forEach(u -> u.setPassword(null));
        return users;
    }

    /** 启用/禁用用户 */
    public User updateStatus(Long adminUserId, Integer userId, Integer status) {
        adminUtils.checkAdmin(adminUserId);
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        user.setStatus(status);
        userMapper.updateUser(user);
        user.setPassword(null);
        return user;
    }

    /** 修改用户权限（普通用户/管理员） */
    public User updatePermission(Long adminUserId, Integer userId, Integer permission) {
        adminUtils.checkAdmin(adminUserId);
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        user.setPermission(permission);
        userMapper.updateUser(user);
        user.setPassword(null);
        return user;
    }
}
