package com.weather.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("city_list")
public class CityList {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String cityName;
    private String cityCode;
    private String province;
}
