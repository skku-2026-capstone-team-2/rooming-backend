package com.skku.zip.domain.locations.entity.value;

import lombok.Getter;

import java.util.Objects;

@Getter
public class RoadAddress {
    private String value;

    public RoadAddress(String value) {
        validate(value);
        this.value = value.trim();
    }

    private void validate(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Road address must not be blank.");
        }
        if (value.length() > 255) {
            throw new IllegalArgumentException("Road address must be 255 characters or fewer.");
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RoadAddress that)) {
            return false;
        }
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
