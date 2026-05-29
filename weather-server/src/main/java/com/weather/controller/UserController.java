package com.weather.controller;

import com.weather.dto.Result;
import com.weather.entity.User;
import com.weather.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户管理接口（管理员）：列表、修改状态、修改权限
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 获取所有用户列表
     *
     * 从 JWT 拦截器注入的 request attribute 中提取当前用户 ID，
     * 传给 Service 层做管理员权限校验。
     *
     * @param request HttpServletRequest（内含 userId 属性，由 JwtInterceptor 注入）
     * @return 用户列表（密码字段已置空）
     */
    @GetMapping
    public Result<List<User>> listUsers(HttpServletRequest request) {
        // 从 JWT 拦截器中获取当前登录用户的 ID
        Long userId = (Long) request.getAttribute("userId");
        try {
            // 查询所有用户，Service 内部会校验当前用户是否为管理员
            return Result.ok(userService.listUsers(userId));
        } catch (IllegalArgumentException e) {
            // 非管理员访问时抛出此异常，返回 403
            return Result.error(403, e.getMessage());
        }
    }

    /**
     * 启用或禁用指定用户
     *
     * 仅管理员可操作。请求体中需传入 status（0=禁用，1=启用）。
     *
     * @param id      目标用户的 ID（路径参数）
     * @param body    请求体，包含 status 字段
     * @param request HttpServletRequest（内含管理员 userId）
     * @return 更新后的用户信息
     */
    @PutMapping("/{id}/status")
    public Result<User> updateStatus(@PathVariable Integer id,
                                     @RequestBody Map<String, Integer> body,
                                     HttpServletRequest request) {
        // 从 JWT 拦截器中获取当前登录管理员的 ID
        Long userId = (Long) request.getAttribute("userId");
        // 校验 status 是否为合法值（0=禁用，1=启用）
        Integer status = body.get("status");
        if (status == null || (status != 0 && status != 1)) {
            return Result.error(400, "状态值不合法");
        }
        try {
            // 更新用户状态，Service 内部会校验管理员权限
            return Result.ok(userService.updateStatus(userId, id, status));
        } catch (IllegalArgumentException e) {
            // 非管理员或目标用户不存在时抛出
            return Result.error(403, e.getMessage());
        }
    }

    /**
     * 修改指定用户的权限等级
     *
     * 仅管理员可操作。请求体中需传入 permission（1=普通用户，2=管理员）。
     *
     * @param id      目标用户的 ID（路径参数）
     * @param body    请求体，包含 permission 字段
     * @param request HttpServletRequest（内含管理员 userId）
     * @return 更新后的用户信息
     */
    @PutMapping("/{id}/permission")
    public Result<User> updatePermission(@PathVariable Integer id,
                                         @RequestBody Map<String, Integer> body,
                                         HttpServletRequest request) {
        // 从 JWT 拦截器中获取当前登录管理员的 ID
        Long userId = (Long) request.getAttribute("userId");
        // 校验 permission 是否为合法值（1=普通用户，2=管理员）
        Integer permission = body.get("permission");
        if (permission == null || (permission != 1 && permission != 2)) {
            return Result.error(400, "权限值不合法");
        }
        try {
            // 更新用户权限，Service 内部会校验管理员权限
            return Result.ok(userService.updatePermission(userId, id, permission));
        } catch (IllegalArgumentException e) {
            // 非管理员或目标用户不存在时抛出
            return Result.error(403, e.getMessage());
        }
    }
}
