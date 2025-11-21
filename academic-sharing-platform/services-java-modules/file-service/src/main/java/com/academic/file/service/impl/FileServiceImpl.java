package com.academic.file.service.impl;

import com.academic.file.service.FileService;
import org.springframework.stereotype.Service;

@Service
public class FileServiceImpl implements FileService {


    @Override
    public void upload(String payload) {
        System.out.println("file upload stub: " + payload);
    }

    @Override
    public String generateDownloadLink(String fileId) {
        return "http://files.local/download/" + fileId;
    }
}
