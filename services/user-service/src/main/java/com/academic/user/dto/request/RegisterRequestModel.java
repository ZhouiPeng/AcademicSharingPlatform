package com.academic.user.dto.request;

import com.academic.user.dto.service.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class RegisterRequestModel
{
    @NotBlank
    private String verificationCode;
    @NotBlank
    private String username;
    @NotBlank
    private String password;
    @NotBlank
    private String email;
    private String displayName;

    public String getVerificationCode()
    {
        return verificationCode;
    }

    public String getUsername()
    {
        return username;
    }

    public String getPassword()
    {
        return password;
    }

    public String getEmail()
    {
        return email;
    }

    public String getDisplayName()
    {
        return displayName;
    }
}
