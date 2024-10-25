package com.kary.spring.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kary.spring.constant.UserConstant;
import com.kary.spring.dto.RegisterDTO;
import com.kary.spring.exception.BaseException;
import com.kary.spring.exception.LoginException;
import com.kary.spring.mapper.UserMapper;
import com.kary.spring.dto.LoginDTO;
import com.kary.spring.entity.User;
import com.kary.spring.service.UserService;
import com.kary.spring.util.JwtUtil;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

  private final UserMapper userMapper;

  private final JwtUtil jwtUtil;

  @Override
  public String login(LoginDTO loginDTO) {
    QueryWrapper<User> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq(UserConstant.ACCOUNT, loginDTO.getAccount());
    User user = userMapper.selectOne(queryWrapper);
    if (Objects.isNull(user)) {
      throw new LoginException("用户不存在", 401);
    }
    if (!loginDTO.getPassword().equals(user.getPassword())) {
      throw new LoginException("密码错误", 401);
    }
    return jwtUtil.getToken(loginDTO);
  }

  @Override
  public void register(RegisterDTO registerDTO) {
    QueryWrapper<User> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq(UserConstant.ACCOUNT, registerDTO);
    User user = userMapper.selectOne(queryWrapper);
    if (Objects.nonNull(user)) {
      throw new BaseException("用户已存在", 500);
    }
    User saveUser = new User().setAccount(registerDTO.getAccount())
        .setPassword(DigestUtils.md5DigestAsHex(registerDTO.getPassword().getBytes()));
    userMapper.insert(saveUser);
  }
}
