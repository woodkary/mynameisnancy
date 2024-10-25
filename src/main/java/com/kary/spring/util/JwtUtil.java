package com.kary.spring.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.kary.spring.constant.UserConstant;
import com.kary.spring.dto.LoginDTO;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

  private static final String KEY = "aiChat";

  private static final String TIMESTAMP = "timestamp";

  //生成token
  public String getToken(LoginDTO loginDTO) {
    return JWT.create()
        .withClaim(UserConstant.ACCOUNT, loginDTO.getAccount())
        .withClaim(UserConstant.PASSWORD, loginDTO.getPassword())
        .withClaim(TIMESTAMP, System.currentTimeMillis())
        .sign(Algorithm.HMAC256(KEY));
  }

  //解析token
  public Map<String, Object> parseToken(String token) {
    DecodedJWT verify = JWT.require(Algorithm.HMAC256(KEY)).build().verify(token);
    Map<String, Object> result = new HashMap<>();
    result.put(UserConstant.ACCOUNT, verify.getClaim(UserConstant.ACCOUNT).asString());
    result.put(UserConstant.PASSWORD, verify.getClaim(UserConstant.PASSWORD).asString());
    result.put(TIMESTAMP, verify.getClaim(TIMESTAMP).asLong().toString());
    return result;
  }
}
