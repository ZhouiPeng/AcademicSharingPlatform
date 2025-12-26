package com.academic.user.dto.request;

import com.academic.user.dto.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequestModel
{
    private String verificationCode;
    private User user;
}
