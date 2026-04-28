package com.skku.zip.domain.property.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminProperty3DRequest {
    @NotBlank private String modelType;
    @NotBlank private String modelUrl;
    private String previewImageUrl;
}
