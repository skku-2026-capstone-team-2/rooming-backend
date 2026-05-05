package com.skku.zip.domain.broker.dto;

import jakarta.validation.constraints.NotBlank;

public record BrokerProfileUpdateRequest(
        @NotBlank
        String officeName,

        @NotBlank
        String registrationNo,

        @NotBlank
        String officePhone,

        @NotBlank
        String officeAddress,

        @NotBlank
        String phoneNumber
) {
}
