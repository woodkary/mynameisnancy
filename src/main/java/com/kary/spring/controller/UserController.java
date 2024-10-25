package com.kary.spring.controller;

import com.kary.spring.dto.LoginDTO;
import com.kary.spring.service.UserService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @PostMapping("/login")
  public String login(@RequestBody LoginDTO loginDTO) {
    return userService.login(loginDTO);
  }

}
