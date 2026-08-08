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
    private final String publicUrl;

    public ImageStoreService(S3Client s3Client,
                              @Value("${CLOUDFLARE_R2_BUCKET_NAME}") String bucketName,
                              @Value("${CLOUDFLARE_R2_PUBLIC_URL:}") String publicUrl) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        // normalize so it always ends with exactly one "/"
        this.publicUrl = publicUrl.isBlank() ? "" : publicUrl.replaceAll("/+$", "") + "/";
    }

    /**
     * Uploads the file to the bucket and returns the full public URL,
     * built from CLOUDFLARE_R2_PUBLIC_URL (not the bucket/S3 endpoint,
     * which isn't publicly readable).
     */
    public String uploadImage(MultipartFile file) throws IOException {
        String fileName = UUID.randomUUID() + "-" + file.getOriginalFilename();
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .contentType(file.getContentType())
                .build();
        s3Client.putObject(putObjectRequest,
                RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        return publicUrl + fileName;
    }

    // unused for now
    public void deleteImage(List<String> fileNames) {
        if (fileNames == null || fileNames.isEmpty()) {
            return;
        }
        List<ObjectIdentifier> keysToProcess = fileNames.stream().map(url -> {
            String cleanedKey = url.replace(publicUrl, "");
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
