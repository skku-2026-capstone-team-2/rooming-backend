package com.skku.zip.domain.broker.dto;

public record BrokerOfficeData(
        Long officeId,
        String officeName,
        String officePhone,
        String officeAddress
) {
}
