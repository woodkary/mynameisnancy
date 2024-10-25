package com.kary.spring.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kary.spring.exception.LoginException;
import com.kary.spring.mapper.UserMapper;
import com.kary.spring.dto.LoginDTO;
import com.kary.spring.entity.User;
import com.kary.spring.service.UserService;
import com.kary.spring.util.JwtUtil;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

  private final UserMapper userMapper;

  private static final String ACCOUNT = "account";

  private final JwtUtil jwtUtil;

  @Override
  public String login(LoginDTO loginDTO) {
    QueryWrapper<User> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq(ACCOUNT, loginDTO.getAccount());
    User user = userMapper.selectOne(queryWrapper);
    if (Objects.isNull(user)) {
      throw new LoginException("用户不存在", 401);
    }
    if (!loginDTO.getPassword().equals(user.getPassword())) {
      throw new LoginException("密码错误", 401);
    }
    return jwtUtil.getToken(loginDTO);
  }
}
