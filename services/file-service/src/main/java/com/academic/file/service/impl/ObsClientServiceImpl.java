package com.academic.file.service.impl;

import com.academic.file.config.ObsProperties;
import com.academic.file.service.ObsClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import com.obs.services.ObsClient;
import com.obs.services.model.CreateBucketRequest;

@Service
public class ObsClientServiceImpl implements ObsClientService {

    private final ObsProperties props;

    @Autowired
    public ObsClientServiceImpl(ObsProperties props) {
        this.props = props;
    }

    private ObsClient createObsClient() {
        ObsClient obsClient = new ObsClient(props.getAccessKey(), props.getSecretKey(), props.getEndpoint());
        if (!obsClient.headBucket(props.getBucket())) {
            CreateBucketRequest request = new CreateBucketRequest();
            request.setBucketName(props.getBucket());
            request.setLocation("cn-north-4");
            obsClient.createBucket(request);
        }
        return obsClient;
    }

    @Override
    public void uploadPdf(MultipartFile file, String filePath) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is empty");
        }

        try (ObsClient obsClient = createObsClient(); InputStream in = file.getInputStream()) {
            obsClient.putObject(props.getBucket(), filePath, in);
        }
    }

    @Override
    public void delete(String filePath) throws Exception {
        try (ObsClient obsClient = createObsClient()) {
            obsClient.deleteObject(props.getBucket(), filePath);
        }
    }

    @Override
    public InputStream getObject(String filePath) throws Exception {
        try (ObsClient obsClient = createObsClient()) {
            return obsClient.getObject(props.getBucket(), filePath).getObjectContent();
        }
    }
}
