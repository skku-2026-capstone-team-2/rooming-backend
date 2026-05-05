package com.skku.zip.domain.locations.entity.converter;

import com.skku.zip.domain.locations.entity.value.RoadAddress;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply=true)
public class RoadAddressConverter implements AttributeConverter<RoadAddress, String> {
    @Override
    public String convertToDatabaseColumn(RoadAddress roadAddress) {
        if (roadAddress == null) {
            return null;
        }
        return roadAddress.getValue();
    }

    @Override
    public RoadAddress convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return new RoadAddress(dbData);
    }
}
