package com.academic.admin.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequest {
    @NotBlank 
    private String type; //USER, CONTENT, OTHER
    @NotBlank 
    private String targetId;
    private String reason;
}
