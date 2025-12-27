package com.academic.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public class VerificationRequestModel
{
    @NotBlank
    private String email;

    public String getEmail()
    {
        return email;
    }
}
