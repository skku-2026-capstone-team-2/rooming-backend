package com.rooming.domain.broker.dto;

import jakarta.validation.constraints.NotBlank;

public record BrokerAdditionalInfoRequest(
        Long officeId,

        @NotBlank
        String registrationNo,

        @NotBlank
        String phoneNumber
) {
}