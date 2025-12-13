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
    public FileUploadDto uploadFile(String uploaderId, MultipartFile file, FileUploadRequest req) {
        String fileId = UUID.randomUUID().toString();
        String fileName = req.getFilename();
        String objectKey = null;
        long size = 0L;

        if (file != null && !file.isEmpty()) {
            size = file.getSize();
            objectKey = "papers/" + (fileName == null ? "unknown" : fileName);
            if (fileRepository.deleteByObjectKey(objectKey) > 0) {
                fileRepository.flush();
            }
            obsClientService.uploadPdf(file, objectKey);
        }
        
        FileEntity.FileEntityBuilder builder = FileEntity.builder()
            .id(fileId)
            .name(fileName)
            .uploaderId(uploaderId)
            .url(req.getUrl())
            .objectKey(objectKey)
            .size(size);
        FileEntity entity = builder.build();
        entity = fileRepository.save(entity);
        return new FileUploadDto(fileId, fileName, size, entity.getCreatedAt().toString());
    }

    @Override
    public FileCheckDto checkFile(String fileId) {
        FileEntity entity = fileRepository.findById(fileId)
            .orElseThrow(() -> new FileException("File not found"));
        return new FileCheckDto(
            entity.getId(),
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
    public FileUploadDto renameFile(String fileId, String newName) {
        FileEntity entity = fileRepository.findById(fileId)
            .orElseThrow(() -> new FileException("File not found: " + fileId));
        String objectKey = "papers/" + (newName == null ? "unknown" : newName);
        obsClientService.changePath(entity.getObjectKey(), objectKey);
        entity.setName(newName);
        entity.setObjectKey(objectKey);
        entity = fileRepository.save(entity);
        return new FileUploadDto(
            entity.getId(),
            entity.getName(),
            entity.getSize(),
            entity.getUpdatedAt().toString()
        );
    }
}
