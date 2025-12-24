package com.academic.admin.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest {
    @NotBlank 
    private String realName;          // 真实姓名
    @NotBlank 
    private String idNumber;          // 身份证号
    @NotBlank 
    private String phone;             // 手机号码
    @NotBlank 
    private String organization;      // 所属单位/学校/研究所
    @NotBlank 
    private String position;          // 职务或职位
    @NotBlank 
    private String applicationReason; // 申请认证的理由或说明
    @NotBlank 
    private String authType;          // 认证类型（如科研人员、学生等）
    private String attachments;       // 相关附件或证明材料的链接或标识
}
