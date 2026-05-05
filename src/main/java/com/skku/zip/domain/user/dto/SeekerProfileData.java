package com.skku.zip.domain.user.dto;

public record SeekerProfileData(
        Long userId,
        String email,
        String name,
        String accountType
) {
}
