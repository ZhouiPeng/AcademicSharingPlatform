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
    private String storageUrl;

    public FileUploadDto(String fileId, String fileName, long size, String uploadTime, String storageUrl) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.size = size;
        this.uploadTime = uploadTime;
        this.storageUrl = storageUrl;
    }
}
