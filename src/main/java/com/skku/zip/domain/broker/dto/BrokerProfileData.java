package com.skku.zip.domain.broker.dto;

public record BrokerProfileData(
        Long brokerId,
        String email,
        String name,
        String accountType,
        String officeName,
        String registrationNo,
        String officePhone,
        String officeAddress,
        String phoneNumber,
        boolean isVerified,
        boolean profileComplete
) {
}
