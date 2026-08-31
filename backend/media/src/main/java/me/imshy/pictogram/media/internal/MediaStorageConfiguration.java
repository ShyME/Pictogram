package me.imshy.pictogram.media.internal;

import java.net.URI;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/** Builds the S3 client the {@link S3BlobStore} talks to, from {@link MediaStorageProperties}. */
@Configuration
@EnableConfigurationProperties(MediaStorageProperties.class)
class MediaStorageConfiguration {

    @Bean
    S3Client mediaS3Client(MediaStorageProperties storage) {
        return S3Client.builder()
                .endpointOverride(URI.create(storage.endpoint()))
                .region(Region.of(storage.region()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(storage.accessKey(), storage.secretKey())))
                .forcePathStyle(storage.pathStyleAccess())
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build();
    }
}
