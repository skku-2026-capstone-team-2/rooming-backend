package com.rooming.domain.broker.dto;

public record BrokerOfficeData(
        Long officeId,
        String officeName,
        String officePhone,
        String officeAddress
) {
}