package com.ustccb.mall.entity;
import lombok.Data;
import java.time.LocalDateTime;
@Data
public class UserAccount {
    private Long id;
    private String username;
    private String password;
    private LocalDateTime createdAt;
}
