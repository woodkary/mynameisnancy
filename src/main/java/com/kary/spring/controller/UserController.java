package com.kary.spring.controller;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.kary.spring.dto.LoginDTO;
import com.kary.spring.dto.RegisterDTO;
import com.kary.spring.entity.R;
import com.kary.spring.service.UserService;
import com.kary.spring.util.JwtUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  private final JwtUtil jwtUtil;

  @PostMapping("/login")
  public R login(@RequestBody LoginDTO loginDTO) {
    return R.successWithData(userService.login(loginDTO));
  }

  @PostMapping("/register")
  public R register(@RequestBody RegisterDTO registerDTO) {
    userService.register(registerDTO);
    return R.commonSuccess();
  }

  @GetMapping("/welcome")
  public R welcome(HttpServletRequest request) {
    String token = request.getHeader("Authorization");
    if (token != null && token.startsWith("Bearer ")) {
      token = token.substring(7); // 提取token值，去掉"Bearer "前缀

      try {
        // 解析token获取用户名
        String username = (String) jwtUtil.parseToken(token).get("account");
        if (username != null) {
          // 用户已登录，返回欢迎信息
          return R.successWithData("Welcome, " + username);
        } else {
          // 用户不存在
          return R.commonFail("401", "Unauthorized");
        }
      } catch (JWTVerificationException e) {
        // token验证失败
        return R.commonFail("401", "Unauthorized");
      }
    } else {
      // 没有提供token
      return R.commonFail("400", "Token is missing");
    }
  }
}
