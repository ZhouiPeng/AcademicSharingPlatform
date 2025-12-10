package com.academic.user.mapper;

import com.academic.user.dto.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User>
{
    User selectUser(String userId);
    int add(User user);
    User selectOneByUserName(String username);
    User selectOneByUserId(String userId);
    int updateUser(User user);
}
