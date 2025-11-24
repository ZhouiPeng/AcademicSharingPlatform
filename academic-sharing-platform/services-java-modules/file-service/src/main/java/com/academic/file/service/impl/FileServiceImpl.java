package com.academic.file.service.impl;

import com.academic.file.dto.*;
import com.academic.file.service.FileService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {

    @Override
    public FileUploadDto uploadFile(String uploaderId, MultipartFile file) {
        String fileId = UUID.randomUUID().toString();
        String fileName = file == null ? null : file.getOriginalFilename();
        long size = file.getSize();
        String uploadTime = DateTimeFormatter.ISO_INSTANT.format(Instant.now());


        return new FileUploadDto(fileId, fileName, size, uploadTime);
    }

    @Override
    public FileCheckDto checkFile(String fileId) {
        return null;
    }

    @Override
    public void deleteFile(String fileId) {
        // Implement file deletion logic here
    }

    @Override
    public FileDownloadDto generateDownloadLink(String fileId) {
        String downloadUrl = "http://files.local/download/" + fileId;
        return new FileDownloadDto(null, 0, downloadUrl);
    }
}
