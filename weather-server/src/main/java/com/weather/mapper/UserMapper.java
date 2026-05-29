package com.weather.mapper;

import com.weather.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户 Mapper（传统 MyBatis XML 方式）
 */
@Mapper
public interface UserMapper {

    /** 按 ID 查询用户 */
    User findById(@Param("id") Integer id);

    /** 按用户名查询用户 */
    User findByUsername(@Param("username") String username);

    /** 查询所有用户，按 ID 升序 */
    List<User> listAllOrderById();

    /** 按 ID 批量查询 */
    List<User> selectBatchIds(@Param("ids") List<Integer> ids);

    /** 按用户名统计数量（检查是否已存在） */
    long countByUsername(@Param("username") String username);

    /** 新增用户（回填自增 ID） */
    int insertUser(User user);

    /** 更新用户信息（非空字段） */
    int updateUser(User user);
}
