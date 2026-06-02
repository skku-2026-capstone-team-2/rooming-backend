package com.rooming.domain.property.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PropertyImageStorageService {

    List<String> storePropertyImages(Long propertyId, List<MultipartFile> images);
}
