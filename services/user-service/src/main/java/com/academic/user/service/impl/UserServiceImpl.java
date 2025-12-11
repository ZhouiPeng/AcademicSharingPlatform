package com.academic.user.service.impl;

import com.academic.user.common.DefaultConfig;
import com.academic.user.common.Role;
import com.academic.user.common.ServiceError;
import com.academic.user.mapper.UserMapper;
import com.academic.user.dto.User;
import com.academic.user.service.UserService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of UserService that uses MyBatis mapper injection.
 * Removed manual SqlSession/SqlSessionFactory usage because Spring Boot + MyBatis starter
 * provides mapper proxies via dependency injection.
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Autowired
    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public String registerNormal(User user) throws Exception
    {
        String uid = UUID.randomUUID().toString();
        user.setUserId(uid);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setAvatarUrl(DefaultConfig.defaultAvatar);
        user.setRole(Role.NORMAL);
        if(user.getDisplayName() == null)
        {
            user.setDisplayName("user" + System.currentTimeMillis());
        }
        if(userMapper.selectOneByUserName(user.getUsername()) != null)
        {
            throw new ServiceError( "用户名已被占用", 0);
        }
        int r = userMapper.add(user);
        return uid;
    }

    @Override
    public User login(User requestUser) throws Exception
    {
        // Use injected mapper directly. add(...) returns int rows affected.
        User user = userMapper.selectOneByUserName(requestUser.getUsername());
        if(!user.getPasswordHash().equals(requestUser.getPasswordHash()))
        {
            throw new ServiceError("用户名或密码错误", 0);
        }
        return user;
    }

    @Override
    public User getCurrent(String userId) throws ServiceError
    {
        User user = userMapper.selectOneByUserId(userId);
        if(user == null)
        {
            throw new ServiceError("用户不存在", 0);
        }
        return user;
    }

    @Override
    public User getById(String userId) throws ServiceError
    {
        User user = userMapper.selectOneByUserId(userId);
        if(user == null)
        {
            throw new ServiceError("用户不存在", 0);
        }
        return user;
    }

    @Override
    public void updateCurrent(User user) throws ServiceError
    {
        user.setUpdatedAt(LocalDateTime.now());
        int r  = userMapper.updateUser(user);
        if(r == 0)
        {
            throw new ServiceError("修改失败", 0);
        }
    }

    @Override
    public void resetPassword(RequestBody req) {

    }

    @Override
    public boolean validateResetCode(String userId, String code) {
        return false;
    }

    @Override
    public void follow(String targetId, String userId) throws ServiceError
    {
        //先判断这两个id是否都存在
        if(userMapper.countByUserId(targetId) != 1 || userMapper.countByUserId(userId) != 1)
        {
            throw new ServiceError("用户不存在", 0);
        }
        if(userMapper.countFollowRecord(targetId, userId) != 0)
        {
            throw new ServiceError("已关注该用户", 0);
        }
        int r = userMapper.addFollowRecord(targetId, userId);
        if(r != 1)
        {
            throw new ServiceError("关注失败", 0);
        }
    }

    @Override
    public void unfollow(String targetId, String userId) throws ServiceError
    {
        //先判断这两个id是否都存在
        if(userMapper.countByUserId(targetId) != 1 || userMapper.countByUserId(userId) != 1)
        {
            throw new ServiceError("用户不存在", 0);
        }
        if(userMapper.countFollowRecord(targetId, userId) != 1)
        {
            throw new ServiceError("未关注该用户", 0);
        }
        int r = userMapper.deleteFollowRecord(targetId, userId);
        if(r != 1)
        {
            throw new ServiceError("关注失败", 0);
        }
    }

    @Override
    public IPage<User> getFollows(String userId, int pageNum, int pageSize) {
        int count = userMapper.countByFolloerId(userId);
        IPage<User> page = new Page<>(pageNum, pageSize, count);

        List<String> userIdList = userMapper.selectPageByFollowerId(page, userId);
        List<User> userList = new ArrayList<>();
        for(String id: userIdList)
        {
            userList.add(userMapper.selectOneByUserId(id));
        }
        page.setRecords(userList);
        return page;
    }

    @Override
    public IPage<User> getFans(String userId, int pageNum, int pageSize) {
        int count = userMapper.countByFolloeeId(userId);
        IPage<User> page = new Page<>(pageNum, pageSize, count);
        List<String> userIdList = userMapper.selectPageByFolloweeId(page, userId);
        List<User> userList = new ArrayList<>();
        for(String id: userIdList)
        {
            userList.add(userMapper.selectOneByUserId(id));
        }
        page.setRecords(userList);
        return page;
    }
}
