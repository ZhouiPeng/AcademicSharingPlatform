package com.academic.file.service;

import com.academic.file.dto.*;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    FileUploadDto uploadFile(String uploaderId, MultipartFile file);
    FileCheckDto checkFile(String fileId);
    FileDownloadDto generateDownloadLink(String fileId);
    void deleteFile(String fileId);
}
