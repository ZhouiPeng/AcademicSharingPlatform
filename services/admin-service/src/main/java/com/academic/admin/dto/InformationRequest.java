package com.academic.admin.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InformationRequest {
    private String title;
    private String content;
    private String targetGroup; // ALL, GROUP_ADMIN, GROUP_USER, or specific user ID
}
