package com.kary.spring.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kary.spring.dto.LoginDTO;
import com.kary.spring.dto.RegisterDTO;
import com.kary.spring.entity.User;

public interface UserService extends IService<User> {

  String login(LoginDTO loginDTO);

  void register(RegisterDTO registerDTO);
}
