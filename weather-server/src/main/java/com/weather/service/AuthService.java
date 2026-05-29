package com.weather.service;

import com.weather.dto.LoginRequest;
import com.weather.dto.RegisterRequest;
import com.weather.entity.User;
import com.weather.mapper.UserMapper;
import com.weather.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证服务：注册、登录、获取当前用户
 */
@Service
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserMapper userMapper, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 用户注册
     *
     * 检查用户名是否唯一，BCrypt 加密密码，写入用户表后返回 JWT Token。
     * 注册成功即自动登录。
     */
    public Map<String, Object> register(RegisterRequest request) {
        // 校验用户名唯一性
        if (userMapper.countByUsername(request.getUsername()) > 0) {
            throw new IllegalArgumentException("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setPermission(1);
        user.setStatus(1);
        userMapper.insertUser(user);

        // 注册成功直接签发 Token
        String token = jwtUtil.generateToken(user.getId().longValue(), user.getUsername());
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("nickname", user.getNickname());
        result.put("permission", user.getPermission());
        return result;
    }

    /**
     * 用户登录
     *
     * BCrypt 校验密码，检查账号是否被禁用，成功返回 JWT Token。
     */
    public Map<String, Object> login(LoginRequest request) {
        User user = userMapper.findByUsername(request.getUsername());

        // 用户名不存在或密码不匹配，返回统一错误信息（防枚举）
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        // 检查账号是否被管理员禁用
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new IllegalArgumentException("账号已被禁用，请联系管理员");
        }

        String token = jwtUtil.generateToken(user.getId().longValue(), user.getUsername());
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("nickname", user.getNickname());
        result.put("permission", user.getPermission());
        return result;
    }

    /** 获取当前用户信息（密码已在 Controller 层置空） */
    public User getCurrentUser(Long userId) {
        return userMapper.findById(Math.toIntExact(userId));
    }
}
