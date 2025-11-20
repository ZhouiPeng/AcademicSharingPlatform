package com.academic.user.service.impl;

import org.springframework.stereotype.Service;

import com.academic.user.dto.UserDto;
import com.academic.user.service.UserService;

@Service
public class UserServiceImpl implements UserService {

    @Override
    public void registerNormal(UserDto dto) {
        System.out.println("user register stub: " + dto.getUsername());
    }

    @Override
    public void login(UserDto dto) {
        System.out.println("user login stub: " + dto.getUsername());
    }

    @Override
    public void logout() {
        System.out.println("user logout stub");
    }
}
