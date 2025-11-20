package com.academic.user.service;

import com.academic.user.dto.UserDto;

public interface UserService {

    void registerNormal(UserDto dto);

    void login(UserDto dto);

    void logout();
}
