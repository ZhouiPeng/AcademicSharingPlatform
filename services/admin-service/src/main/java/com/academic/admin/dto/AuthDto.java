package com.academic.admin.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthDto {
    private String userId;            // 用户ID
    private String formId;            // 表单ID
    private String realName;          // 真实姓名
    private String idNumber;          // 身份证号
    private String phone;             // 手机号码
    private String organization;      // 所属单位/学校/研究所
    private String position;          // 职务或职位
    private String applicationReason; // 申请认证的理由或说明
    private String authType;          // 认证类型（如科研人员、学生等）
    private String attachments;       // 相关附件或证明材料的链接或标识
    private String createdAt;         // 申请时间
}
