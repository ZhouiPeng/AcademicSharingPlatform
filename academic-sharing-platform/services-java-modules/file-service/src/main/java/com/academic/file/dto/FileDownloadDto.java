package com.academic.file.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileDownloadDto {
    private String fileName;
    private int size;
    private String downloadUrl; 
}
