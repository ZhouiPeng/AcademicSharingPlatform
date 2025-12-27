package com.academic.admin.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessRequest {
    @NotBlank 
    private String status;
    @NotBlank 
    private String remarks;
}
