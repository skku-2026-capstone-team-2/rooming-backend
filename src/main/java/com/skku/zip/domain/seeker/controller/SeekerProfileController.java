package com.skku.zip.domain.seeker.controller;

import com.skku.zip.common.dto.ApiResponse;
import com.skku.zip.domain.seeker.dto.SeekerProfileData;
import com.skku.zip.domain.seeker.entity.Seeker;
import com.skku.zip.domain.user.entity.AccountType;
import com.skku.zip.security.principal.PrincipalDetails;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user/seeker")
public class SeekerProfileController {

    @GetMapping("/me")
    public ApiResponse<SeekerProfileData> getMe(@AuthenticationPrincipal PrincipalDetails principalDetails) {
        if (principalDetails == null || principalDetails.getUser().getAccountType() != AccountType.SEEKER) {
            throw new AccessDeniedException("Seeker account is required.");
        }

        Seeker seeker = (Seeker) principalDetails.getUser();
        return ApiResponse.success(
                new SeekerProfileData(
                        seeker.getId(),
                        seeker.getEmail(),
                        seeker.getName(),
                        seeker.getAccountType().name()
                ),
                "User profile fetched."
        );
    }
}
