package com.kary.spring.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kary.spring.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
