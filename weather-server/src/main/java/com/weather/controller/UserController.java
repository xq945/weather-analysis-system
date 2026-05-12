package com.weather.controller;

import com.weather.dto.Result;
import com.weather.entity.User;
import com.weather.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public Result<List<User>> listUsers(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        try {
            return Result.ok(userService.listUsers(userId));
        } catch (IllegalArgumentException e) {
            return Result.error(403, e.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    public Result<User> updateStatus(@PathVariable Integer id,
                                     @RequestBody Map<String, Integer> body,
                                     HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Integer status = body.get("status");
        if (status == null || (status != 0 && status != 1)) {
            return Result.error(400, "状态值不合法");
        }
        try {
            return Result.ok(userService.updateStatus(userId, id, status));
        } catch (IllegalArgumentException e) {
            return Result.error(403, e.getMessage());
        }
    }

    @PutMapping("/{id}/permission")
    public Result<User> updatePermission(@PathVariable Integer id,
                                         @RequestBody Map<String, Integer> body,
                                         HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Integer permission = body.get("permission");
        if (permission == null || (permission != 1 && permission != 2)) {
            return Result.error(400, "权限值不合法");
        }
        try {
            return Result.ok(userService.updatePermission(userId, id, permission));
        } catch (IllegalArgumentException e) {
            return Result.error(403, e.getMessage());
        }
    }
}
