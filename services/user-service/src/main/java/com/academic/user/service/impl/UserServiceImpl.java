package com.academic.user.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.academic.user.common.DefaultConfig;
import com.academic.user.common.Role;
import com.academic.user.common.ServiceError;
import com.academic.user.dto.ResetRequest;
import com.academic.user.dto.User;
import com.academic.user.mapper.UserMapper;
import com.academic.user.service.UserService;
import com.academic.user.service.mail.MailService;

/**
 * Implementation of UserService that uses MyBatis mapper injection. Removed
 * manual SqlSession/SqlSessionFactory usage because Spring Boot + MyBatis
 * starter provides mapper proxies via dependency injection.
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    // in-memory store for reset tokens: userId -> token entry
    private final ConcurrentHashMap<String, TokenEntry> resetTokens = new ConcurrentHashMap<>();
    private final MailService mailService;

    @Autowired
    public UserServiceImpl(UserMapper userMapper, MailService mailService) {
        this.userMapper = userMapper;
        this.mailService = mailService;
    }

    @Override
    public String registerNormal(User user) throws Exception {
        String uid = UUID.randomUUID().toString();
        user.setUserId(uid);
        user.setCreateTime(LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        user.setUpdateTime(LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        user.setAvatarUrl(DefaultConfig.defaultAvatar);
        user.setRole(Role.NORMAL);
        if (user.getDisplayName() == null) {
            user.setDisplayName("user" + System.currentTimeMillis());
        }
        if (userMapper.selectOneByUserName(user.getUsername()) != null) {
            throw new ServiceError("用户名已被占用", 0);
        }
        int r = userMapper.add(user);
        return uid;
    }

    @Override
    public User login(User requestUser) throws Exception {
        // Use injected mapper directly. add(...) returns int rows affected.
        User user = userMapper.selectOneByUserName(requestUser.getUsername());
        if (!user.getPasswordHash().equals(requestUser.getPasswordHash())) {
            throw new ServiceError("用户名或密码错误", 0);
        }
        return user;
    }

    @Override
    public User getCurrent(String userId) throws ServiceError {
        User user = userMapper.selectOneByUserId(userId);
        if (user == null) {
            throw new ServiceError("用户不存在", 0);
        }
        return user;
    }

    @Override
    public User getById(String userId) throws ServiceError {
        User user = userMapper.selectOneByUserId(userId);
        if (user == null) {
            throw new ServiceError("用户不存在", 0);
        }
        return user;
    }

    @Override
    public void updateCurrent(User user) throws ServiceError {
        user.setUpdateTime(LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        int r = userMapper.updateUser(user);
        if (r == 0) {
            throw new ServiceError("修改失败", 0);
        }
    }

    @Override
    public void resetPassword(ResetRequest req) throws ServiceError {
        if (req == null) {
            throw new ServiceError("请求体为空", 0);
        }
        // locate user by userId or username
        User user = null;
        if (req.getUserId() != null && !req.getUserId().isEmpty()) {
            user = userMapper.selectOneByUserId(req.getUserId());
        }
        if (user == null && req.getUsername() != null && !req.getUsername().isEmpty()) {
            user = userMapper.selectOneByUserName(req.getUsername());
        }
        if (user == null) {
            throw new ServiceError("用户不存在", 0);
        }
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            throw new ServiceError("用户未设置邮箱，无法发送验证码", 0);
        }

        // generate 6-digit numeric code
        int codeInt = ThreadLocalRandom.current().nextInt(100000, 1000000);
        String code = String.format("%06d", codeInt);
        long expireAt = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10);
        resetTokens.put(user.getUserId(), new TokenEntry(code, expireAt));

        // send email (or log)
        mailService.sendResetCode(user.getEmail(), code);
    }

    @Override
    public boolean validateResetCode(String userId, String code) {
        if (userId == null || userId.isEmpty() || code == null || code.isEmpty()) {
            return false;
        }
        TokenEntry entry = resetTokens.get(userId);
        if (entry == null) {
            return false;
        }
        if (System.currentTimeMillis() > entry.expireAt) {
            resetTokens.remove(userId);
            return false;
        }
        if (entry.code.equals(code)) {
            resetTokens.remove(userId);
            return true;
        }
        return false;
    }

    @Override
    public void follow(String scholarId) {

    }

    @Override
    public void unfollow(String scholarId) {

    }

    @Override
    public List<User> getFollows(int pageNum, int pageSize) {
        return null;
    }

    @Override
    public List<User> getFans(int pageNum, int pageSize) {
        return null;
    }
}

class TokenEntry {

    final String code;
    final long expireAt;

    TokenEntry(String code, long expireAt) {
        this.code = code;
        this.expireAt = expireAt;
    }
}
