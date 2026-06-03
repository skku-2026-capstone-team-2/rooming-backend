package com.rooming.domain.property.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class S3PropertyImageStorageServiceTest {

    private final S3Client s3Client = mock(S3Client.class);

    @Test
    void uploadsPropertyImageAndReturnsS3Url() {
        S3PropertyImageStorageService storageService = storageService("");
        MockMultipartFile image = new MockMultipartFile(
                "images",
                "main room.png",
                "image/png",
                new byte[]{1, 2, 3}
        );

        List<String> urls = storageService.storePropertyImages(103L, List.of(image));

        assertThat(urls).hasSize(1);
        assertThat(urls.getFirst())
                .startsWith("https://rooming-property-image.s3.ap-northeast-2.amazonaws.com/properties/103/images/")
                .endsWith("-main_room.png");

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        PutObjectRequest request = requestCaptor.getValue();
        assertThat(request.bucket()).isEqualTo("rooming-property-image");
        assertThat(request.key()).startsWith("properties/103/images/");
        assertThat(request.contentType()).isEqualTo("image/png");
        assertThat(request.contentLength()).isEqualTo(3L);
    }

    @Test
    void deletesPropertyImageByStoredS3Url() {
        S3PropertyImageStorageService storageService = storageService("");

        storageService.deletePropertyImage(
                "https://rooming-property-image.s3.ap-northeast-2.amazonaws.com/properties/103/images/main.png"
        );

        ArgumentCaptor<DeleteObjectRequest> requestCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(requestCaptor.capture());
        DeleteObjectRequest request = requestCaptor.getValue();
        assertThat(request.bucket()).isEqualTo("rooming-property-image");
        assertThat(request.key()).isEqualTo("properties/103/images/main.png");
    }

    @Test
    void usesPublicBaseUrlWhenConfigured() {
        S3PropertyImageStorageService storageService = storageService("https://cdn.rooming.cloud/property-images/");
        MockMultipartFile image = new MockMultipartFile(
                "images",
                "main.png",
                "image/png",
                new byte[]{1}
        );

        List<String> urls = storageService.storePropertyImages(103L, List.of(image));

        assertThat(urls.getFirst()).startsWith("https://cdn.rooming.cloud/property-images/properties/103/images/");
    }

    private S3PropertyImageStorageService storageService(String publicBaseUrl) {
        return new S3PropertyImageStorageService(
                s3Client,
                "rooming-property-image",
                "ap-northeast-2",
                publicBaseUrl
        );
    }
}
