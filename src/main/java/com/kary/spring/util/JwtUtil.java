package com.kary.spring.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.kary.spring.constant.UserConstant;
import com.kary.spring.dto.LoginDTO;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.kary.spring.entity.TokenUserClaim;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

  private static final String KEY = "aiChat";

  private static final String TIMESTAMP = "timestamp";
  private static final long ACCESS_TOKEN_EXPIRATION = 60 * 60 * 1000; // 1小时
  private static final String ISSUER =  "kary";


  //生成token
  public String getToken(TokenUserClaim userClaim) {
    return JWT.create()
        .withIssuer(ISSUER)
        .withClaim("id", userClaim.getId())
        .withClaim("account", userClaim.getAccount())
        .withClaim("timestamp", System.currentTimeMillis())
        .withExpiresAt(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION))
        .sign(Algorithm.HMAC256(KEY));
  }

  //解析token
  public Map<String, Object> parseToken(String token) {
    try {
      DecodedJWT decodedJWT = JWT.require(Algorithm.HMAC256(KEY)).build().verify(token);

      Map<String, Object> result = new HashMap<>();
      result.put("id", decodedJWT.getClaim("id").asLong());
      result.put("account", decodedJWT.getClaim("account").asString());
      result.put("timestamp", decodedJWT.getClaim("timestamp").asLong());
      result.put("expiresAt", decodedJWT.getExpiresAt());

      return result;
    } catch (TokenExpiredException e) {
      // 处理 token 过期的情况
      throw new RuntimeException("Token 已过期", e);
    } catch (Exception e) {
      // 处理其他异常
      throw new RuntimeException("Token 无效", e);
    }
  }


}
