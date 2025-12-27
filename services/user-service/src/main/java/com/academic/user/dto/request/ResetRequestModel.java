package com.academic.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public class ResetRequestModel
{
    @NotBlank
    private String password;
    @NotBlank
    private String code;

    public String getPassword()
    {
        return password;
    }

    public String getCode()
    {
        return code;
    }
}
