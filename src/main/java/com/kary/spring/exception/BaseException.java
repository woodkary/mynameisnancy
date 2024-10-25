package com.kary.spring.exception;


import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class BaseException extends RuntimeException {
  public String message;
  public Integer code;
  public Object data;

  public BaseException(String message, Integer code) {
    super(message);
    this.message = message;
    this.code = code;
  }
}
