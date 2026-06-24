package com.ustccb.mall.mapper;
import com.ustccb.mall.entity.UserAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
@Mapper
public interface UserMapper {
    UserAccount findByUsername(@Param("username") String username);
}
