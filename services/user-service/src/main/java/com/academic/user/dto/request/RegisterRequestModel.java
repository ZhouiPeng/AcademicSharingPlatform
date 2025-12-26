package com.academic.user.dto.request;

import com.academic.user.dto.User;

public class RegisterRequestModel
{
    private String verificationCode;
    private User user;

    public String getVerificationCode()
    {
        return verificationCode;
    }

    public User getUser()
    {
        return user;
    }
}
