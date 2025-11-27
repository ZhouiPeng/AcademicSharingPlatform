package com.academic.file.service;

import java.io.InputStream;

import org.springframework.web.multipart.MultipartFile;

public interface ObsClientService {
    void uploadPdf(MultipartFile file, String filePath) throws Exception;
    void delete(String filePath) throws Exception;
    InputStream getObject(String filePath) throws Exception;
}
