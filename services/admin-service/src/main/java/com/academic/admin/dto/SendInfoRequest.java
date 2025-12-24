package com.academic.admin.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendInfoRequest {
    @NotBlank 
    private String title;
    @NotBlank 
    private String content;
    @NotBlank 
    private String targetGroup; // ALL, GROUP_ADMIN, GROUP_USER, or specific user ID
}
