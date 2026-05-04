package com.skku.zip.domain.locations.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply=true)
public class RoadAddressConverter implements AttributeConverter<RoadAddress, String> {
    @Override
    public String convertToDatabaseColumn(RoadAddress roadAddress) {
        return roadAddress.getValue();
    }

    @Override
    public RoadAddress convertToEntityAttribute(String dbData) {
        return new RoadAddress(dbData);
    }
}
