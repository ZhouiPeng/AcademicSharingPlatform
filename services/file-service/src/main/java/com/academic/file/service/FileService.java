package com.academic.file.service;

import com.academic.file.dto.*;

public interface FileService {
    FileUploadDto uploadFile(String uploaderId, FileUploadRequest url);
    void deleteFile(String fileId);
    FileUploadDto renameFile(String fileId, String newName);
    FileCheckDto checkFile(String fileId);
}
