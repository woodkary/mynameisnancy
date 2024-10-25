package com.kary.spring.entity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class R {

  private String code;
  private String message;
  private Object data;

  public static R commonSuccess() {
    return R.builder().code("200").message("请求成功").build();
  }

  public static R successWithData(Object data) {
    return R.builder().code("200").message("请求成功").data(data).build();
  }

  public static R commonFail(String code, String message) {
    return R.builder().code(code).message(message).build();
  }
}
