package com.projeto.th_piscinas_api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ImageStoreService {

    private final S3Client s3Client;
    private final String bucketName;

    public ImageStoreService(S3Client s3Client, @Value("${CLOUDFLARE_R2_BUCKET_NAME}") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    public String uploadImage(MultipartFile file) throws IOException {
        String fileName = UUID.randomUUID() + "-" + file.getOriginalFilename();
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .contentType(file.getContentType())
                .build();
        s3Client.putObject(putObjectRequest,
                RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        return fileName;
    }

    // unused for now
    public void deleteImage(List<String> fileNames) {
        if (fileNames == null || fileNames.isEmpty()) {
            return;
        }
        String prefix = "https://cdn.carldev.online/th-piscinas/";
        List<ObjectIdentifier> keysToProcess = fileNames.stream().map(url -> {
            String cleanedKey = url.replace(prefix, "");
            return ObjectIdentifier.builder().key(cleanedKey).build();
        }).toList();
        Delete delete = Delete.builder().objects(keysToProcess).build();
        try {
            DeleteObjectsRequest deleteObjectRequest = DeleteObjectsRequest.builder()
                    .bucket(bucketName)
                    .delete(delete)
                    .build();
            s3Client.deleteObjects(deleteObjectRequest);
        } catch (S3Exception e) {
            log.error("Erro ao deletar {} do Cloudflare r2 {}", delete,
                    e.awsErrorDetails().errorMessage());
        }
    }
}
