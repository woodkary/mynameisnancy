package com.kary.spring.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class LoginException extends BaseException{

  public LoginException(String message, Integer code) {
    super(message, code);
  }

  public LoginException(String message, Integer code, Object data) {
    super(message, code, data);
  }
}
