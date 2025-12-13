package com.academic.file.service;

import com.academic.file.dto.*;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    FileUploadDto uploadFile(String uploaderId, MultipartFile file, FileUploadRequest url);
    void deleteFile(String fileId);
    FileUploadDto renameFile(String fileId, String newName);
    FileCheckDto checkFile(String fileId);
}
