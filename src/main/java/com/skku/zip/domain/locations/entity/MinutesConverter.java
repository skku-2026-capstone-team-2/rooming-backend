package com.skku.zip.domain.locations.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class MinutesConverter implements AttributeConverter<Minutes, Integer> {
    @Override
    public Integer convertToDatabaseColumn(Minutes minutes) {
        return minutes.getValue();
    }
    @Override
    public Minutes convertToEntityAttribute(Integer dbData) {
        return new Minutes(dbData);
    }
}
