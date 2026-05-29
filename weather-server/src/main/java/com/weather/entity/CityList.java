package com.weather.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 城市 ID 对照表（和风天气城市代码）
 */
@Data
@TableName("city_list")
public class CityList {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String cityName;    // 城市名
    private String cityCode;    // 和风天气城市代码
    private String province;    // 所属省份
}
