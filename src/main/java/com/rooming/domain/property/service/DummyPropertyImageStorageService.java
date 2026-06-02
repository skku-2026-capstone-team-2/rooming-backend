package com.rooming.domain.property.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
public class DummyPropertyImageStorageService implements PropertyImageStorageService {

    private static final String DUMMY_BASE_URL = "https://dummy-storage.rooming.local/properties";

    @Override
    public List<String> storePropertyImages(Long propertyId, List<MultipartFile> images) {
        return images.stream()
                .map(image -> DUMMY_BASE_URL + "/" + propertyId + "/images/" + UUID.randomUUID() + "-" + safeFileName(image))
                .toList();
    }

    private String safeFileName(MultipartFile image) {
        String originalFileName = image.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            return "property-image";
        }
        return originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
