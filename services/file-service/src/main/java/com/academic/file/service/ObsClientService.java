package com.academic.file.service;

import java.io.InputStream;

import org.springframework.web.multipart.MultipartFile;

public interface ObsClientService {
    void uploadPdf(MultipartFile file, String filePath);
    void delete(String filePath);
    InputStream getObject(String filePath);
}
