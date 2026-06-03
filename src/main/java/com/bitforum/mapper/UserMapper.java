package com.bitforum.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bitforum.entity.User;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 什么都不用写，CRUD 自动拥有
    // BaseMapper  自带的方法：insert()、deleteById()、updateById()、selectById()、selectList()、selectPage()
}
