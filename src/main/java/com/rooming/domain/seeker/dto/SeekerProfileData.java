package com.rooming.domain.seeker.dto;

public record SeekerProfileData(
        Long userId,
        String email,
        String name,
        String accountType
) {
}