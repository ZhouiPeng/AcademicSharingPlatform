package com.academic.user.service;

import com.academic.user.common.ServiceError;
import com.academic.user.dto.User;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
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

    void follow(String targetId, String userId) throws ServiceError;

    void unfollow(String targetId, String userId) throws ServiceError;

    IPage<User> getFollows(String userId, int pageNum, int pageSize);

    IPage<User> getFans(String userId, int pageNum, int pageSize);
}
