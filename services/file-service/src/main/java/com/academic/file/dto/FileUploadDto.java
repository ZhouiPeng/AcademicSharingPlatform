package com.academic.file.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadDto {
    private String fileId;
    private String fileName;
    private long size;
    private String uploadTime;
}
