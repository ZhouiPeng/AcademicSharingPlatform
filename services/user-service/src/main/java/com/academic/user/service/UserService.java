package com.academic.user.service;

import java.security.NoSuchAlgorithmException;

import org.springframework.stereotype.Service;

import com.academic.user.common.ServiceError;
import com.academic.user.dto.service.User;
import com.baomidou.mybatisplus.core.metadata.IPage;

@Service
public interface UserService {

    String registerNormal(User user) throws Exception;

    User login(User user) throws Exception;

    User getCurrent(String userId) throws ServiceError;

    User getById(String userId) throws ServiceError;

    User getByUsername(String username) throws ServiceError;

    void updateCurrent(User user) throws ServiceError;

    String generateVerificationCode(String userId, String mail) throws Exception;

    void validateVerificationCode(String validateId, String code) throws ServiceError;

    void resetPassword(String userId, String newPasswordHash) throws ServiceError, NoSuchAlgorithmException, Exception;

    void follow(String targetId, String userId) throws ServiceError;

    void unfollow(String targetId, String userId) throws ServiceError;

    IPage<User> getFollows(String userId, int pageNum, int pageSize);

    IPage<User> getFans(String userId, int pageNum, int pageSize);

    IPage<User> getUsers(int pageNum, int pageSize);

    IPage<User> getUsersByRole(int pageNum, int pageSize, String role) throws ServiceError;

    int getUsersNum();
}
