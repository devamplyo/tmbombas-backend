package com.projeto.th_piscinas_api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
public class CloudflareConfig {

    @Value("${CLOUDFLARE_R2_ENDPOINT}")
    private String endpoint;

    @Value("${CLOUDFLARE_R2_ACCESS_KEY}")
    private String accessKey;

    @Value("${CLOUDFLARE_R2_SECRET_KEY}")
    private String secretKey;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                )).region(Region.US_EAST_1).build();
    }
}
