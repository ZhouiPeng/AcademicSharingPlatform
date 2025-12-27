package com.academic.user.dto.response;

import com.academic.user.dto.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseModel
{
    private String token;
    private String expiresIn;
    private User user;

}
