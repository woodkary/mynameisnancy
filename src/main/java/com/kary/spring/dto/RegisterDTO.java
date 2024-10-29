package com.kary.spring.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(chain = true)
public class RegisterDTO {

  private String account;
  private String password;

  //TODO其他信息
}
