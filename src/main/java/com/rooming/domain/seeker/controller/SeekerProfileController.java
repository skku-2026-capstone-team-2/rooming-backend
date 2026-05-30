package com.rooming.domain.seeker.controller;

import com.rooming.common.dto.ApiResponse;
import com.rooming.common.exception.ForbiddenException;
import com.rooming.domain.seeker.dto.SeekerProfileData;
import com.rooming.domain.seeker.entity.Seeker;
import com.rooming.domain.user.entity.AccountType;
import com.rooming.security.principal.PrincipalDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user/seeker")
public class SeekerProfileController {

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<SeekerProfileData>> getMe(
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        if (principalDetails == null || principalDetails.getUser().getAccountType() != AccountType.SEEKER) {
            throw new ForbiddenException("Seeker account is required.");
        }

        Seeker seeker = (Seeker) principalDetails.getUser();
        return ResponseEntity.ok(ApiResponse.success(
                new SeekerProfileData(
                        seeker.getId(),
                        seeker.getEmail(),
                        seeker.getName(),
                        seeker.getAccountType().name()
                ),
                "User profile fetched."
        ));
    }
}