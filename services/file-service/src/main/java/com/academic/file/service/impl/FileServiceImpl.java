package com.academic.file.service.impl;

import com.academic.file.dto.*;
import com.academic.file.service.FileService;
import com.academic.file.service.ObsClientService;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {

    private final ObsClientService obsClientService;

    @Autowired
    public FileServiceImpl(ObsClientService obsClientService) {
        this.obsClientService = obsClientService;
    }

    @Override
    public FileUploadDto uploadFile(String uploaderId, MultipartFile file) {
        String fileId = UUID.randomUUID().toString();
        String fileName = file == null ? null : file.getOriginalFilename();
        long size = file.getSize();
        String uploadTime = DateTimeFormatter.ISO_INSTANT.format(Instant.now());

        // build object key, keep original extension if present
        String ext = "";
        if (fileName != null && fileName.contains(".")) {
            ext = fileName.substring(fileName.lastIndexOf('.'));
        }
        String url = null;
        try {
            url = obsClientService.uploadPdf(file);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload to OBS", e);
        }

        FileUploadDto dto = new FileUploadDto(fileId, fileName, size, uploadTime, url);
        return dto;
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
