package com.academic.file.service;

import com.academic.file.dto.*;
import jakarta.servlet.http.HttpServletResponse;

public interface FileService {
    FileDto uploadFile(String uploaderId, FileRequest url);
    void deleteFile(String fileId);
    FileDto modifyFile(String uploaderId, String fileId, FileRequest req);
    FileDto checkFile(String fileId);
    void downloadFile(String fileId, HttpServletResponse response);
}
