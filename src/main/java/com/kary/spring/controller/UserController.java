package com.kary.spring.controller;

import com.kary.spring.dto.LoginDTO;
import com.kary.spring.dto.RegisterDTO;
import com.kary.spring.entity.R;
import com.kary.spring.service.UserService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @PostMapping("/login")
  public R login(@RequestBody LoginDTO loginDTO) {
    return R.successWithData(userService.login(loginDTO));
  }

  @PostMapping("/register")
  public R register(@RequestBody RegisterDTO registerDTO) {
    userService.register(registerDTO);
    return R.commonSuccess();
  }

}
