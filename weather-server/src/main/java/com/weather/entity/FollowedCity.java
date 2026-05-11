package com.weather.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("followed_city")
public class FollowedCity {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private String city;
    private LocalDateTime createdAt;
}
