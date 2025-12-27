package com.academic.user.dto.response;

import com.academic.user.dto.service.User;

public class LoginResponseModel
{
    private String token;
    private String expiresIn;
    private User user;

    public LoginResponseModel(String token, String expiresIn, User user)
    {
        this.token = token;
        this.expiresIn = expiresIn;
        this.user = user;
    }
}
