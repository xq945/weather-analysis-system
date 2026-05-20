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

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final LoginRateLimiter loginRateLimiter;

    public AuthController(AuthService authService, LoginRateLimiter loginRateLimiter) {
        this.authService = authService;
        this.loginRateLimiter = loginRateLimiter;
    }

    @PostMapping("/register")
    public Result<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        Map<String, Object> result = authService.register(request);
        return Result.ok(result);
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) {
        loginRateLimiter.check(httpRequest);
        try {
            Map<String, Object> result = authService.login(request);
            loginRateLimiter.recordSuccess(httpRequest);
            return Result.ok(result);
        } catch (IllegalArgumentException e) {
            loginRateLimiter.recordFailure(httpRequest);
            return Result.error(400, e.getMessage());
        }
    }

    @GetMapping("/me")
    public Result<User> me(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User user = authService.getCurrentUser(userId);
        user.setPassword(null);
        return Result.ok(user);
    }
}
