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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;


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
     * 登录成功后返回到此页
     *
     * @param callback 登录用户的信息
     * @return
     */
    @RequestMapping("/callback")
    public Object login(AuthCallback callback) {
        AuthRequest authRequest = getAuthRequest();
        // 打印返回的授权信息
        //System.out.println(callback.getCode());
        //根据返回的参数，执行登录请求（获取用户信息）
        AuthResponse<AuthUser> authResponse = authRequest.login(callback);
        //打印用户信息
        System.out.println("用户的UnionID：" + authResponse.getData().getUuid());

        if (authResponse.ok()) {
            AuthUser authUser = authResponse.getData();
            String uuid = authUser.getUuid();

            try {
                // 尝试第三方登录
                String token = userService.thirdPartyLogin(uuid);
                // 返回token
                return ResponseEntity.ok().body(token);
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
     * @return 各种请求的结果
     */
    private AuthRequest getAuthRequest() {
        return new AuthGiteeRequest(AuthConfig.builder()
                .clientId("2e5fbb45a366e4b775896fdb6ad30a28c3413bdab4a5311c2e880b6186b1c141")
                .clientSecret("6c1aba227a0305f15255cb915bf119ec4a02ca28ecd78b4c7dfe167bb45a72f4")
                .redirectUri("http://localhost:8080/oauth/callback")
                .build());
    }
}
