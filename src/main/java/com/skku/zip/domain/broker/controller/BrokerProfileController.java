package com.skku.zip.domain.broker.controller;

import com.skku.zip.common.dto.ApiResponse;
import com.skku.zip.domain.broker.dto.BrokerProfileData;
import com.skku.zip.domain.broker.entity.Broker;
import com.skku.zip.domain.broker.service.BrokerProfileService;
import com.skku.zip.domain.user.entity.AccountType;
import com.skku.zip.security.principal.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user/broker")
public class BrokerProfileController {

    private final BrokerProfileService brokerProfileService;

    @GetMapping("/me")
    public ApiResponse<BrokerProfileData> getMe(@AuthenticationPrincipal PrincipalDetails principalDetails) {
        Broker broker = requireBroker(principalDetails);
        return ApiResponse.success(
                brokerProfileService.toProfile(broker),
                "Broker profile fetched."
        );
    }

    private Broker requireBroker(PrincipalDetails principalDetails) {
        if (principalDetails == null || principalDetails.getUser().getAccountType() != AccountType.BROKER) {
            throw new AccessDeniedException("Broker account is required.");
        }
        return (Broker) principalDetails.getUser();
    }
}
