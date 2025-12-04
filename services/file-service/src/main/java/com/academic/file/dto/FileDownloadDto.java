package com.academic.file.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
public class FileDownloadDto {
    private String fileName;
    private int size;
    private String downloadUrl; 

    public FileDownloadDto(String fileName, int size, String downloadUrl) {
        this.fileName = fileName;
        this.size = size;
        this.downloadUrl = downloadUrl;
    }
}
