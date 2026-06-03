package com.rooming.domain.property.service;

import com.rooming.common.exception.UpstreamServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class S3PropertyImageStorageService implements PropertyImageStorageService {

    private final S3Client s3Client;
    private final String bucketName;
    private final String region;
    private final String publicBaseUrl;

    public S3PropertyImageStorageService(
            @Qualifier("propertyImageS3Client") S3Client s3Client,
            @Value("${rooming.property-images.s3.bucket}") String bucketName,
            @Value("${rooming.property-images.s3.region}") String region,
            @Value("${rooming.property-images.public-base-url:}") String publicBaseUrl
    ) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.region = region;
        this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.trim();
    }

    @Override
    public List<String> storePropertyImages(Long propertyId, List<MultipartFile> images) {
        return images.stream()
                .map(image -> uploadPropertyImage(propertyId, image))
                .toList();
    }

    @Override
    public void deletePropertyImage(String imageUrl) {
        String objectKey = objectKeyFromUrl(imageUrl);
        if (objectKey == null || objectKey.isBlank()) {
            log.warn("Skipping S3 delete because property image URL is not recognized: {}", imageUrl);
            return;
        }

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build());
        } catch (SdkException exception) {
            throw new UpstreamServiceException("Property image delete failed.", exception);
        }
    }

    private String uploadPropertyImage(Long propertyId, MultipartFile image) {
        String objectKey = objectKey(propertyId, image);

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectKey)
                            .contentType(image.getContentType())
                            .contentLength(image.getSize())
                            .build(),
                    RequestBody.fromInputStream(image.getInputStream(), image.getSize())
            );
            return publicUrl(objectKey);
        } catch (IOException | SdkException exception) {
            throw new UpstreamServiceException("Property image upload failed.", exception);
        }
    }

    private String objectKey(Long propertyId, MultipartFile image) {
        return "properties/" + propertyId + "/images/" + UUID.randomUUID() + "-" + safeFileName(image);
    }

    private String safeFileName(MultipartFile image) {
        String originalFileName = image.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            return "property-image";
        }
        return originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String publicUrl(String objectKey) {
        String baseUrl = normalizedPublicBaseUrl();
        if (baseUrl != null) {
            return baseUrl + "/" + objectKey;
        }
        return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + objectKey;
    }

    private String normalizedPublicBaseUrl() {
        if (publicBaseUrl.isBlank()) {
            return null;
        }
        return publicBaseUrl.replaceAll("/+$", "");
    }

    private String objectKeyFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }

        String normalizedBaseUrl = normalizedPublicBaseUrl();
        if (normalizedBaseUrl != null && imageUrl.startsWith(normalizedBaseUrl + "/")) {
            return imageUrl.substring(normalizedBaseUrl.length() + 1);
        }

        try {
            URI uri = URI.create(imageUrl);
            String path = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/+", "");
            String host = uri.getHost();
            if (host == null || path.isBlank()) {
                return null;
            }

            if (host.equals(bucketName + ".s3.amazonaws.com")
                    || host.startsWith(bucketName + ".s3.")
                    || host.startsWith(bucketName + ".s3-")) {
                return path;
            }

            if (host.endsWith(".amazonaws.com") && path.startsWith(bucketName + "/")) {
                return path.substring(bucketName.length() + 1);
            }

            if (path.startsWith("properties/")) {
                return path;
            }
            return null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
