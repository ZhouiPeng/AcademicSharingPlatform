package com.academic.file.service.impl;

import com.academic.file.dto.*;
import com.academic.file.entity.FileEntity;
import com.academic.file.exception.FileException;
import com.academic.file.repository.FileRepository;
import com.academic.file.service.FileService;
import com.academic.file.service.ObsClientService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class FileServiceImpl implements FileService {

    private final ObsClientService obsClientService;
    private final FileRepository fileRepository;

    public FileServiceImpl(ObsClientService obsClientService, FileRepository fileRepository) {
        this.obsClientService = obsClientService;
        this.fileRepository = fileRepository;
    }

    @Override
    @Transactional
    public FileDto uploadFile(String uploaderId, FileRequest req) {
        String fileId = UUID.randomUUID().toString();
        String fileName = req.getFileName();
        String fileType = req.getFileType();
        MultipartFile file = req.getFile();
        String objectKey = null;
        String url = req.getUrl();
        long size = 0L;

        if (fileRepository.findByTypeAndName(fileType, fileName).isPresent()) {
            throw new FileException("File already exists");
        }

        if (file != null && !file.isEmpty()) {
            size = file.getSize();
            objectKey = uploaderId + "/" + fileType + "/" + fileName;
            obsClientService.uploadPdf(file, objectKey);
        }
        
        FileEntity.FileEntityBuilder builder = FileEntity.builder()
            .id(fileId)
            .type(fileType)
            .name(fileName)
            .uploaderId(uploaderId)
            .url(url)
            .objectKey(objectKey)
            .size(size);
        FileEntity entity = builder.build();
        entity = fileRepository.save(entity);
        return new FileDto(fileId, fileType, fileName, size, uploaderId, url, entity.getCreatedAt().toString(), entity.getUpdatedAt().toString(), null);
    }

    @Override
    public FileDto checkFile(String fileId) {
        FileEntity entity = fileRepository.findById(fileId)
            .orElseThrow(() -> new FileException("File not found"));
        return new FileDto(
            entity.getId(),
            entity.getType(),
            entity.getName(),
            entity.getSize(),
            entity.getUrl(),
            entity.getUploaderId(),
            entity.getCreatedAt().toString(),
            entity.getUpdatedAt().toString(),
            null
        );
    }

    @Override
    @Transactional
    public void deleteFile(String fileId) {
        FileEntity entity = fileRepository.findById(fileId)
            .orElseThrow(() -> new FileException("File not found: " + fileId));
        String objectKey = entity.getObjectKey();
        obsClientService.delete(objectKey);
        fileRepository.delete(entity);
    }

    @Override
    public FileDto modifyFile(String uploaderId, String fileId, FileRequest req) {
        FileEntity entity = fileRepository.findById(fileId)
            .orElseThrow(() -> new FileException("File not found: " + fileId));
        String fileName = req.getFileName();
        String fileType = req.getFileType();
        MultipartFile file = req.getFile();
        String url = req.getUrl();
        String objectKey = null;
        long size = 0L;

        if (entity.getObjectKey() != null) {
            obsClientService.delete(entity.getObjectKey());
        }
        if (file != null && !file.isEmpty()) {
            size = file.getSize();
            objectKey = uploaderId + "/" + fileType + "/" + fileName;
            obsClientService.uploadPdf(file, objectKey);
        }

        entity.setType(fileType);
        entity.setName(fileName);
        entity.setUploaderId(uploaderId);
        entity.setUrl(url);
        entity.setObjectKey(objectKey);
        entity.setSize(size);
        entity = fileRepository.save(entity);
        return new FileDto(fileId, fileType, fileName, size, uploaderId, url, entity.getCreatedAt().toString(), entity.getUpdatedAt().toString(), null);
    }

    @Override
    public void downloadFile(String fileId, HttpServletResponse response) {
        FileEntity entity = fileRepository.findById(fileId)
            .orElseThrow(() -> new FileException("File not found: " + fileId));
        String objectKey = entity.getObjectKey();
        try (InputStream input = obsClientService.getObject(objectKey)) {
            input.transferTo(response.getOutputStream());
        } catch (IOException e) {
            throw new FileException("Download failed", e);
        }
    }
}
