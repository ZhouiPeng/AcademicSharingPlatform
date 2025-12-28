package com.academic.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "验证码响应模型")
public class VerificationResponseModel
{
    @Schema(description = "验证码ID，用于后续验证", example = "abc123def456")
    private String validateId;

    public String getValidateId()
    {
        return validateId;
    }

    public void setValidateId(String validateId)
    {
        this.validateId = validateId;
    }
}
