package com.academic.user.dto.response;

import com.academic.user.dto.service.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description="登录响应模型")
public class LoginResponseModel
{
    @Schema(description="JWT令牌", example="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;
    
    @Schema(description="过期时间", example="3600")
    private String expiresIn;
    
    @Schema(description="用户信息")
    private User user;

    public LoginResponseModel(String token, String expiresIn, User user)
    {
        this.token = token;
        this.expiresIn = expiresIn;
        this.user = user;
    }

    public String getToken()
    {
        return token;
    }

    public String getExpiresIn()
    {
        return expiresIn;
    }

    public User getUser()
    {
        return user;
    }
}
