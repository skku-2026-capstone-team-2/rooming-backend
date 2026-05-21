package com.skku.zip.domain.seeker.dto;

public record SeekerProfileData(
        Long userId,
        String email,
        String name,
        String accountType
) {
}
