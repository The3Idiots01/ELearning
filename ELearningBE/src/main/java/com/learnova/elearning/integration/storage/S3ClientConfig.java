package com.learnova.elearning.integration.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * S3 client cho object storage tương thích S3 (Cloudflare R2). Chỉ tạo bean khi
 * learnova.storage.provider=s3. R2 bắt buộc path-style (cert *.r2... chỉ phủ 1 cấp
 * subdomain nên virtual-hosted-style sẽ lỗi TLS).
 */
@Configuration
@ConditionalOnProperty(name = "learnova.storage.provider", havingValue = "s3")
public class S3ClientConfig {

    private final S3Configuration serviceConfig = S3Configuration.builder()
            .pathStyleAccessEnabled(true)
            .build();

    @Bean
    public S3Client s3Client(StorageProperties properties) {
        StorageProperties.S3 s3 = properties.getS3();
        return S3Client.builder()
                .endpointOverride(URI.create(s3.getEndpoint()))
                .region(Region.of(s3.getRegion()))
                .credentialsProvider(credentials(s3))
                .serviceConfiguration(serviceConfig)
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(StorageProperties properties) {
        StorageProperties.S3 s3 = properties.getS3();
        return S3Presigner.builder()
                .endpointOverride(URI.create(s3.getEndpoint()))
                .region(Region.of(s3.getRegion()))
                .credentialsProvider(credentials(s3))
                .serviceConfiguration(serviceConfig)
                .build();
    }

    private StaticCredentialsProvider credentials(StorageProperties.S3 s3) {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(s3.getAccessKey(), s3.getSecretKey()));
    }
}
