package com.rooming.domain.property.service;

import com.rooming.common.exception.BadRequestException;
import com.rooming.common.exception.ForbiddenException;
import com.rooming.domain.broker.entity.Broker;
import com.rooming.domain.property.dto.PropertyImageResponse;
import com.rooming.domain.property.entity.Property;
import com.rooming.domain.property.repository.PropertyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropertyServiceTest {

    private final PropertyRepository propertyRepository = mock(PropertyRepository.class);
    private final PropertyImageStorageService propertyImageStorageService = mock(PropertyImageStorageService.class);
    private final PropertyService propertyService = new PropertyService(propertyRepository, propertyImageStorageService);

    @Test
    void ownerBrokerCanAppendPropertyImages() {
        Broker owner = broker(7L);
        Property property = property(owner, List.of("/images/101_existing.png"));
        List<MultipartFile> imageFiles = List.of(
                imageFile("main.png"),
                imageFile("bath.png")
        );
        when(propertyRepository.findById(101L)).thenReturn(Optional.of(property));
        when(propertyImageStorageService.storePropertyImages(101L, imageFiles))
                .thenReturn(List.of("/images/101_main.png", "/images/101_bath.png"));

        List<PropertyImageResponse> images = propertyService.updateImages(
                101L,
                owner,
                imageFiles
        );

        assertThat(property.getImageUrls())
                .containsExactly("/images/101_existing.png", "/images/101_main.png", "/images/101_bath.png");
        assertThat(images)
                .extracting(PropertyImageResponse::getImageId, PropertyImageResponse::getImageUrl, PropertyImageResponse::getImageOrder)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1, "/images/101_existing.png", 1),
                        org.assertj.core.groups.Tuple.tuple(2, "/images/101_main.png", 2),
                        org.assertj.core.groups.Tuple.tuple(3, "/images/101_bath.png", 3)
                );
        verify(propertyImageStorageService).storePropertyImages(101L, imageFiles);
    }

    @Test
    void differentBrokerCannotSavePropertyImages() {
        Broker owner = broker(7L);
        Broker otherBroker = broker(8L);
        when(propertyRepository.findById(101L)).thenReturn(Optional.of(property(owner, List.of())));

        assertThatThrownBy(() -> propertyService.updateImages(
                101L,
                otherBroker,
                List.of(imageFile("main.png"))
        ))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Only the broker who registered this property can update property images.");
    }

    @Test
    void ownerBrokerCanDeletePropertyImage() {
        Broker owner = broker(7L);
        Property property = property(owner, List.of("/images/101_main.png", "/images/101_bath.png"));
        when(propertyRepository.findById(101L)).thenReturn(Optional.of(property));

        List<PropertyImageResponse> images = propertyService.deleteImage(101L, owner, 1);

        assertThat(property.getImageUrls()).containsExactly("/images/101_bath.png");
        assertThat(images)
                .extracting(PropertyImageResponse::getImageId, PropertyImageResponse::getImageUrl, PropertyImageResponse::getImageOrder)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(1, "/images/101_bath.png", 1));
        verify(propertyImageStorageService).deletePropertyImage("/images/101_main.png");
    }

    @Test
    void nonImageFileIsRejected() {
        Broker owner = broker(7L);
        when(propertyRepository.findById(101L)).thenReturn(Optional.of(property(owner, List.of())));

        assertThatThrownBy(() -> propertyService.updateImages(
                101L,
                owner,
                List.of(new MockMultipartFile("images", "readme.txt", "text/plain", "not image".getBytes()))
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Only image files are allowed.");
    }

    private Broker broker(Long id) {
        Broker broker = Broker.builder()
                .email("broker" + id + "@example.test")
                .name("Broker " + id)
                .provider("google")
                .loginId("google_broker_" + id)
                .build();
        ReflectionTestUtils.setField(broker, "id", id);
        return broker;
    }

    private Property property(Broker broker, List<String> imageUrls) {
        Property property = Property.builder()
                .propertyId(101L)
                .imageUrls(new ArrayList<>(imageUrls))
                .build();
        property.assignBroker(broker);
        return property;
    }

    private MultipartFile imageFile(String fileName) {
        return new MockMultipartFile("images", fileName, "image/png", new byte[]{1, 2, 3});
    }
}
