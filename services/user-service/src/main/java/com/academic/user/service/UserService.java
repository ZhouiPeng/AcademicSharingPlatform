package com.academic.user.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.academic.user.common.ServiceError;
import com.academic.user.dto.ResetRequest;
import com.academic.user.dto.User;

@Service
public interface UserService {

    String registerNormal(User user) throws Exception;

    User login(User user) throws Exception;

    User getCurrent(String userId) throws ServiceError;

    User getById(String userId) throws ServiceError;

    void updateCurrent(User user) throws ServiceError;

    void resetPassword(ResetRequest req) throws ServiceError;

    boolean validateResetCode(String userId, String code);

    void follow(String scholarId);

    void unfollow(String scholarId);

    List<User> getFollows(int pageNum, int pageSize);

    List<User> getFans(int pageNum, int pageSize);
}
