package com.weather.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户关注的城市
 */
@Data
@TableName("followed_city")
public class FollowedCity {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;         // 用户 ID
    private String city;            // 城市名称
    private LocalDateTime createdAt;
}
