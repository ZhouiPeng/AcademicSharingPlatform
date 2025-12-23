package com.academic.file.service.impl;

import com.academic.file.config.ObsProperties;
import com.academic.file.service.ObsClientService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import com.obs.services.ObsClient;
import com.obs.services.model.CreateBucketRequest;
import com.academic.file.exception.FileException;

@Service
public class ObsClientServiceImpl implements ObsClientService {

    private final ObsProperties props;

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
    public void uploadPdf(MultipartFile file, String filePath) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is required and must not be empty");
        }
        try (ObsClient obsClient = createObsClient(); InputStream in = file.getInputStream()) {
            obsClient.putObject(props.getBucket(), filePath, in);
        } catch (Exception e) {
            throw new FileException("Failed to upload file", e);
        }
    }

    @Override
    public void delete(String filePath) {
        try (ObsClient obsClient = createObsClient()) {
            obsClient.deleteObject(props.getBucket(), filePath);
        } catch (Exception e) {
            throw new FileException("Failed to delete file", e);
        }
    }

    @Override
    public InputStream getObject(String filePath) {
        try (ObsClient obsClient = createObsClient()) {
            return obsClient.getObject(props.getBucket(), filePath).getObjectContent();
        } catch (Exception e) {
            throw new FileException("Failed to get object", e);
        }
    }
}
