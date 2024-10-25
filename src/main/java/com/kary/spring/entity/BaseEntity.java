package com.kary.spring.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import jakarta.persistence.criteria.CriteriaBuilder.In;
import java.util.Date;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BaseEntity {
  @TableField("create_time")
  private Date createTime;

  @TableField("create_by")
  private String createBy;

  @TableField("update_time")
  private Date updateTime;

  @TableField("update_by")
  private String updateBy;

  @TableField("deleted")
  @TableLogic
  private Integer deleted;   //
}
