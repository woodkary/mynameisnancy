package com.kary.spring.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@Accessors(chain = true)
@Table(name = "user")
public class User extends BaseEntity{

  @TableId
  private Long Id;
  private String account;
  private String password;
  private String name;
}
