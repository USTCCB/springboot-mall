package com.ustccb.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户账户实体类
 */
@Data
@TableName("user_account")
public class UserAccount {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private LocalDateTime createdAt;
}
