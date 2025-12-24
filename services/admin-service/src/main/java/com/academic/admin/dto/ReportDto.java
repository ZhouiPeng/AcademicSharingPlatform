package com.academic.admin.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportDto {
    private String reporterId;
    private String reportId;
    private String type;
    private String targetId;
    private String reason;
    private String createdAt;
}
