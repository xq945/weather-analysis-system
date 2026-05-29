package com.weather.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户
 */
@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String username;        // 登录用户名
    private String password;        // BCrypt 加密密码
    private String nickname;        // 显示昵称
    private Integer permission;     // 权限: 1=普通用户, 2=管理员
    private Integer status;         // 状态: 0=禁用, 1=正常
    private LocalDateTime createdAt;
}
