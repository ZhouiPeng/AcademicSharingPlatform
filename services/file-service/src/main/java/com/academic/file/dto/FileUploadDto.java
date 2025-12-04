package com.academic.file.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
public class FileUploadDto {
    private String fileId;
    private String fileName;
    private long size;
    private String uploadTime;
}
