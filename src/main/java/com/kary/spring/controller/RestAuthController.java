package com.kary.spring.controller;

import com.kary.spring.entity.User;
import com.kary.spring.exception.LoginException;
import com.kary.spring.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthGiteeRequest;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.request.AuthRequest;
import me.zhyd.oauth.utils.AuthStateUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Duration;


@RestController
@RequestMapping("/oauth")
public class RestAuthController {
    private final UserService userService;

    public RestAuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 跳转至登录页面
     *
     * @param response 页面跳转
     * @throws IOException
     */
    @RequestMapping("/login")
    public void renderAuth(HttpServletResponse response) throws IOException {
        AuthRequest authRequest = getAuthRequest();
        //打印生成的链接
        String authorizeUrl = authRequest.authorize(AuthStateUtils.createState());
        //System.out.println(authorizeUrl);
        response.sendRedirect(authorizeUrl);
    }

    /**
     * 登录回调
     *
     * @param callback 回调参数
     * @return 登录结果
     */
    @RequestMapping("/callback")
    public Object login(AuthCallback callback) {
        AuthRequest authRequest = getAuthRequest();
        AuthResponse<AuthUser> authResponse = authRequest.login(callback);

        if (authResponse.ok()) {
            AuthUser authUser = authResponse.getData();
            String uuid = authUser.getUuid();

            try {
                // 尝试第三方登录
                String token = userService.thirdPartyLogin(uuid);
                // 使用 ResponseCookie 设置 HTTP-only 的 token
                ResponseCookie cookie = ResponseCookie.from("token", token)
                        .httpOnly(true)
                        .secure(true) // 使用 HTTPS 时设置为 true
                        .path("/")
                        .maxAge(Duration.ofHours(1)) // 设置有效期
                        .build();

                // 将 token 设置在响应的 Cookie 中
                return ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, cookie.toString())
                        .body("登录成功");
            } catch (LoginException e) {
                // 处理登录异常
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
            }
        } else {
            // 处理授权失败的情况
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("授权失败");
        }

    }




    /**
     * 授权接口类
     *
     */
    private AuthRequest getAuthRequest() {
        return new AuthGiteeRequest(AuthConfig.builder()
                .clientId("2e5fbb45a366e4b775896fdb6ad30a28c3413bdab4a5311c2e880b6186b1c141")
                .clientSecret("6c1aba227a0305f15255cb915bf119ec4a02ca28ecd78b4c7dfe167bb45a72f4")
                .redirectUri("http://localhost:8080/oauth/callback")
                .build());
    }
}
