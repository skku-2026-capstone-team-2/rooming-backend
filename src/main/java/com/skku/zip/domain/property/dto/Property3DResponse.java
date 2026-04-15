package com.skku.zip.domain.property.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Property3DResponse {
    private Long propertyId;
    private Boolean has3DModel;
    private String modelType;   // ex) "spline"
    private String modelUrl;
    private String previewImageUrl;
}
