package com.academic.file.service.impl;

import com.academic.file.config.ObsProperties;
import com.academic.file.service.ObsClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.S3Configuration;
import java.io.InputStream;
import java.net.URI;

@Service
public class ObsClientServiceImpl implements ObsClientService {

    private final ObsProperties props;

    @Autowired
    public ObsClientServiceImpl(ObsProperties props) {
        this.props = props;
    }

    private S3Client createClient() {
        AwsBasicCredentials creds = AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey());
        Region region = Region.of("cn-north-4");
        String endpoint = props.getEndpoint();
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(creds))
                .region(region)
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    private String getObjectKey(String original) {
        String filename;
        if (original == null || original.isBlank()) {
            filename = java.util.UUID.randomUUID().toString();
        } else {
            filename = java.nio.file.Paths.get(original).getFileName().toString();
        }
        return "files/" + filename;
    }

    @Override
    public String uploadPdf(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is empty");
        }

        String objectKey = getObjectKey(file.getOriginalFilename());

        try (S3Client s3 = createClient(); InputStream in = file.getInputStream()) {
            PutObjectRequest req = PutObjectRequest.builder()
                    .bucket(props.getBucket())
                    .key(objectKey)
                    .contentType(file.getContentType())
                    .build();

            s3.putObject(req, RequestBody.fromInputStream(in, file.getSize()));

            String url = String.format("%s/%s/%s", props.getEndpoint(), props.getBucket(), objectKey);
            return url;
        }
    }

    @Override
    public void delete(String filename) throws Exception {
        try (S3Client s3 = createClient()) {
            DeleteObjectRequest req = DeleteObjectRequest.builder()
                    .bucket(props.getBucket())
                    .key("files/" + filename)
                    .build();
            s3.deleteObject(req);
        }
    }
}
