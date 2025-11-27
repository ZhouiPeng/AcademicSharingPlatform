package com.academic.file.service;

import org.springframework.web.multipart.MultipartFile;

public interface ObsClientService {
    String uploadPdf(MultipartFile file) throws Exception;
    void delete(String fileName) throws Exception;
}
