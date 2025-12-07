package com.academic.file.service.impl;

import com.academic.file.dto.*;
import com.academic.file.entity.FileEntity;
import com.academic.file.exception.FileException;
import com.academic.file.repository.FileRepository;
import com.academic.file.service.FileService;
import com.academic.file.service.ObsClientService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    public FileUploadDto uploadFile(String uploaderId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required and must not be empty");
        }
        String fileId = UUID.randomUUID().toString();
        String fileName = file == null ? null : file.getOriginalFilename();
        long size = file == null ? 0L : file.getSize();
        String uploadTime = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        String objectKey = "papers/" + (fileName == null ? "unknown" : fileName);
        if (fileRepository.deleteByObjectKey(objectKey) > 0) {
            fileRepository.flush();
        }
        obsClientService.uploadPdf(file, objectKey);
        FileEntity.FileEntityBuilder builder = FileEntity.builder()
            .id(fileId)
            .name(fileName)
            .uploaderId(uploaderId)
            .bucket("team13-file")
            .objectKey(objectKey)
            .size(size)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now());
        FileEntity entity = builder.build();
        fileRepository.save(entity);
        return new FileUploadDto(fileId, fileName, size, uploadTime);
    }

    @Override
    public FileCheckDto checkFile(String fileId) {
        FileEntity entity = fileRepository.findById(fileId)
            .orElseThrow(() -> new FileException("File not found"));
        return new FileCheckDto(
            entity.getId(),
            entity.getName(),
            entity.getSize() == null ? 0L : entity.getSize(),
            Collections.emptyList(),
            entity.getUploaderId() == null ? null : String.valueOf(entity.getUploaderId()),
            entity.getCreatedAt() == null ? null : entity.getCreatedAt().toString(),
            entity.getUpdatedAt() == null ? null : entity.getUpdatedAt().toString(),
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
    public FileUploadDto renameFile(String fileId, String newName) {
        FileEntity entity = fileRepository.findById(fileId)
            .orElseThrow(() -> new FileException("File not found: " + fileId));
        String objectKey = "papers/" + (newName == null ? "unknown" : newName);
        obsClientService.changePath(entity.getObjectKey(), objectKey);
        entity.setName(newName);
        entity.setObjectKey(objectKey);
        entity.setUpdatedAt(LocalDateTime.now());
        fileRepository.save(entity);
        return new FileUploadDto(
            entity.getId(),
            entity.getName(),
            entity.getSize() == null ? 0L : entity.getSize(),
            entity.getUpdatedAt() == null ? null : entity.getUpdatedAt().toString()
        );
    }
}
