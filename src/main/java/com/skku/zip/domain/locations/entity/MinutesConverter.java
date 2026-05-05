package com.skku.zip.domain.locations.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class MinutesConverter implements AttributeConverter<Minutes, Integer> {
    @Override
    public Integer convertToDatabaseColumn(Minutes minutes) {
        if (minutes == null) {
            return null;
        }
        return minutes.getValue();
    }
    @Override
    public Minutes convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        return new Minutes(dbData);
    }
}
