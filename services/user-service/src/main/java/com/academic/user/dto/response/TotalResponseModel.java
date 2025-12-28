package com.academic.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description="用户总数响应模型")
public class TotalResponseModel
{
    @Schema(description="用户总数", example="100")
    private int total;

    public TotalResponseModel(int total)
    {
        this.total = total;
    }

    public int getTotal()
    {
        return total;
    }
}
