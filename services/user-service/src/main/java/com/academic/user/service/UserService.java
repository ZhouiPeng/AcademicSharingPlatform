package com.academic.user.service;

import org.springframework.stereotype.Service;

import com.academic.user.common.ServiceError;
import com.academic.user.dto.User;
import com.baomidou.mybatisplus.core.metadata.IPage;

@Service
public interface UserService {

    String registerNormal(User user) throws Exception;

    User login(User user) throws Exception;

    User getCurrent(String userId) throws ServiceError;

    User getById(String userId) throws ServiceError;

    void updateCurrent(User user) throws ServiceError;

    String generateVerificationCode(String userId, String mail) throws ServiceError;

    boolean validateVerificationCode(String userId, String code);

    boolean resetPassword(String userId, String code);

    void follow(String targetId, String userId) throws ServiceError;

    void unfollow(String targetId, String userId) throws ServiceError;

    IPage<User> getFollows(String userId, int pageNum, int pageSize);

    IPage<User> getFans(String userId, int pageNum, int pageSize);

}
