package com.skku.zip.domain.broker.dto;

public record BrokerProfileData(
        Long brokerId,
        String email,
        String name,
        String accountType,
        Long officeId,
        String officeName,
        String registrationNo,
        String officePhone,
        String officeAddress,
        String phoneNumber,
        boolean hasVerificationDocument,
        String verificationDocumentFileName,
        boolean isVerified,
        boolean profileComplete
) {
}
