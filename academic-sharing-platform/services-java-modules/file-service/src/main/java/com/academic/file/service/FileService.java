package com.academic.file.service;

public interface FileService {

    void upload(String payload);

    String generateDownloadLink(String fileId);
}
