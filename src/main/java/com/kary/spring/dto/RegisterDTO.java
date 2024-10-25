package com.kary.spring.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RegisterDTO {

  private String account;
  private String password;

  //TODO其他信息
}
