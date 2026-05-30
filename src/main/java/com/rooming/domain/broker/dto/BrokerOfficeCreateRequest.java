package com.rooming.domain.broker.dto;

import jakarta.validation.constraints.NotBlank;

public record BrokerOfficeCreateRequest(
        @NotBlank
        String officeName,

        @NotBlank
        String officePhone,

        @NotBlank
        String officeAddress
) {
}