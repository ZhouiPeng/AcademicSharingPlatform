package com.academic.file.service;

import com.academic.file.dto.*;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    FileUploadDto uploadFile(String uploaderId, MultipartFile file);
    void deleteFile(String fileId);
    FileCheckDto modifyFile(String fileId);
    FileCheckDto checkFile(String fileId);
}
