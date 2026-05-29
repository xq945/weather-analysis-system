package com.weather.controller;

import com.weather.config.LoginRateLimiter;
import com.weather.dto.LoginRequest;
import com.weather.dto.RegisterRequest;
import com.weather.dto.Result;
import com.weather.entity.User;
import com.weather.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 认证接口：注册、登录、获取当前用户
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final LoginRateLimiter loginRateLimiter;

    public AuthController(AuthService authService, LoginRateLimiter loginRateLimiter) {
        this.authService = authService;
        this.loginRateLimiter = loginRateLimiter;
    }

    /**
     * 用户注册
     *
     * 注册成功自动返回 JWT Token，前端可直接进入首页。
     *
     * @param request 注册信息（用户名、密码、可选昵称）
     * @return token + 用户信息
     */
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        Map<String, Object> result = authService.register(request);
        return Result.ok(result);
    }

    /**
     * 用户登录
     *
     * 校验用户名密码，成功后返回 JWT Token。
     * 登录失败会触发 IP 级别的计数器，连续失败 5 次后锁定 5 分钟。
     *
     * @param request      登录信息
     * @param httpRequest  HTTP 请求（用于获取客户端 IP）
     * @return token + 用户信息
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) {
        // 检查 IP 是否已被临时封锁
        loginRateLimiter.check(httpRequest);
        try {
            Map<String, Object> result = authService.login(request);
            // 登录成功，清除该 IP 的失败记录
            loginRateLimiter.recordSuccess(httpRequest);
            return Result.ok(result);
        } catch (IllegalArgumentException e) {
            // 登录失败，记录一次失败尝试
            loginRateLimiter.recordFailure(httpRequest);
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 获取当前登录用户信息
     *
     * @param request HTTP 请求（内含 userId 属性，由 JwtInterceptor 注入）
     * @return 用户信息（密码字段已置空）
     */
    @GetMapping("/me")
    public Result<User> me(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User user = authService.getCurrentUser(userId);
        user.setPassword(null);
        return Result.ok(user);
    }
}
