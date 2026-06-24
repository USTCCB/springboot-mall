package com.ustccb.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ustccb.mall.entity.UserAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户 Mapper：继承 MyBatis-Plus 的 BaseMapper
 */
@Mapper
public interface UserMapper extends BaseMapper<UserAccount> {
    UserAccount findByUsername(@Param("username") String username);
}
