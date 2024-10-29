package com.kary.spring.exceptionHandler;

import com.kary.spring.entity.R;
import com.kary.spring.exception.LoginException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(LoginException.class)
  public R handlerLoginException(LoginException ex) {
    return R.commonFail("401", ex.getMessage());
  }
}
