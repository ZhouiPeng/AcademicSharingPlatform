package com.academic.admin.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InformationDto {
    private String id;
    private String title;
    private String content;
    private String state;
    private String updatedAt;
}
