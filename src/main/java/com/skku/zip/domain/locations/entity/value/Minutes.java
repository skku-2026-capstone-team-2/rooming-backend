package com.skku.zip.domain.locations.entity.value;

import lombok.Getter;

@Getter
public class Minutes {
    private Integer value;

    public Minutes(Integer value) {
        validate(value);
        this.value = value;
    }

    private void validate(Integer value) {
        if (value < 0) {
            throw new IllegalArgumentException();
        }
    }
}
