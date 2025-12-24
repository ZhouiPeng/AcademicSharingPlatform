package com.academic.user.mapper;

import com.academic.user.dto.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User>
{
    User selectUser(String userId);

    int add(User user);

    User selectOneByUserName(String username);

    User selectOneByUserId(String userId);

    int updateUser(User user);

    int countByUserId(String userId);

    int countFollowRecord(String targetId, String userId);

    int addFollowRecord(String targetId, String userId);

    int deleteFollowRecord(String targetId, String userId);

    List<String> selectPageByFollowerId(IPage<User>page, String userId);

    List<String> selectPageByFolloweeId(IPage<User>page,String userId);

    int countByFolloerId(String userId);

    int countByFolloeeId(String userId);

    int count();
}
