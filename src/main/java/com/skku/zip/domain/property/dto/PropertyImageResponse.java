package com.skku.zip.domain.property.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PropertyImageResponse {
    private Integer imageId;
    private String imageUrl;
    private Integer imageOrder;
}




