package com.academic.user.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.academic.user.common.DefaultConfig;
import com.academic.user.common.Role;
import com.academic.user.common.ServiceError;
import com.academic.user.dto.User;
import com.academic.user.mapper.UserMapper;
import com.academic.user.service.UserService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

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
    private final JavaMailSender mailSender;

    @Autowired
    public UserServiceImpl(UserMapper userMapper, JavaMailSender mailSender) {
        this.userMapper = userMapper;
        this.mailSender = mailSender;
    }

    @Override
    public String registerNormal(User user) throws Exception {
        String uid = UUID.randomUUID().toString();
        user.setUserId(uid);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
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
        user.setUpdatedAt(LocalDateTime.now());
        int r = userMapper.updateUser(user);
        if (r == 0) {
            throw new ServiceError("修改失败", 0);
        }
    }

    @Override
    public String generateVerificationCode(String userId, String mail) throws ServiceError {
        String validateId;
        if (userId != null) {
            mail = userMapper.selectOneByUserId(userId).getEmail();
        }
        validateId = String.format("%04d", ThreadLocalRandom.current().nextInt(1000, 10000));

        // allow request to provide an alternate mail address; fall back to user's email
        // if client provided a verification code in request, use it; otherwise generate one
        int codeInt = ThreadLocalRandom.current().nextInt(100000, 1000000);
        String code = String.format("%06d", codeInt);
        long expireAt = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10);
        resetTokens.put(validateId, new TokenEntry(code, expireAt));

        // send verification code via SMTP using configured JavaMailSender
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            String from = System.getenv("SMTP_USER");
            if (from != null && !from.isEmpty()) {
                message.setFrom(from);
            }
            message.setTo(mail);
            message.setSubject("[user-service] 验证码 / Verification Code");
            message.setText("您的验证码是: " + code + "。有效期 10 分钟。\nIf you didn't request this, please ignore this email.");
            mailSender.send(message);
        } catch (Exception e) {
            // fallback
            throw new ServiceError("发送验证码失败", 0);
        }
        return validateId;
    }

    @Override
    public void validateVerificationCode(String validateId, String code) throws ServiceError {
        if (validateId == null || validateId.isEmpty() || code == null || code.isEmpty()) {
            throw new ServiceError("参数错误", 0);
        }
        TokenEntry entry = resetTokens.get(validateId);
        if (entry == null) {
            throw new ServiceError("参数错误", 0);
        }
        if (System.currentTimeMillis() > entry.expireAt) {
            resetTokens.remove(validateId);
            throw new ServiceError("参数错误", 0);
        }
        if (entry.code.equals(code)) {
            resetTokens.remove(validateId);
            return;
        }
        throw new ServiceError("参数错误", 0);
    }

    @Override
    public void resetPassword(String userId, String newPasswordHash) throws ServiceError {
        User user = userMapper.selectOneByUserId(userId);
        if (user == null) {
            throw new ServiceError("用户不存在", 0);
        }
        user.setPasswordHash(newPasswordHash);
        user.setUpdatedAt(LocalDateTime.now());
        int r = userMapper.updateUser(user);
        if (r == 0) {
            throw new ServiceError("修改失败", 0);
        }
    }

    @Override
    public void follow(String targetId, String userId) throws ServiceError {
        //先判断这两个id是否都存在
        if (userMapper.countByUserId(targetId) != 1 || userMapper.countByUserId(userId) != 1) {
            throw new ServiceError("用户不存在", 0);
        }
        if (userMapper.countFollowRecord(targetId, userId) != 0) {
            throw new ServiceError("已关注该用户", 0);
        }
        int r = userMapper.addFollowRecord(targetId, userId);
        if (r != 1) {
            throw new ServiceError("关注失败", 0);
        }
    }

    @Override
    public void unfollow(String targetId, String userId) throws ServiceError {
        //先判断这两个id是否都存在
        if (userMapper.countByUserId(targetId) != 1 || userMapper.countByUserId(userId) != 1) {
            throw new ServiceError("用户不存在", 0);
        }
        if (userMapper.countFollowRecord(targetId, userId) != 1) {
            throw new ServiceError("未关注该用户", 0);
        }
        int r = userMapper.deleteFollowRecord(targetId, userId);
        if (r != 1) {
            throw new ServiceError("关注失败", 0);
        }
    }

    @Override
    public IPage<User> getFollows(String userId, int pageNum, int pageSize) {
        int count = userMapper.countByFolloerId(userId);
        IPage<User> page = new Page<>(pageNum, pageSize, count);

        List<String> userIdList = userMapper.selectPageByFollowerId(page, userId);
        List<User> userList = new ArrayList<>();
        for (String id : userIdList) {
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
        for (String id : userIdList) {
            userList.add(userMapper.selectOneByUserId(id));
        }
        page.setRecords(userList);
        return page;
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
