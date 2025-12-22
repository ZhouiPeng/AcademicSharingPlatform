package com.academic.file.dto;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadRequest {
    @NotBlank
    private String fileType;
    @NotBlank
    private String fileName;
    private String url;
    private MultipartFile file;
}
