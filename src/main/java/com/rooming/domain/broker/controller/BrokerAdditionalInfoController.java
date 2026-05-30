package com.rooming.domain.broker.controller;

import com.rooming.common.dto.ApiResponse;
import com.rooming.common.exception.ForbiddenException;
import com.rooming.domain.broker.dto.BrokerAdditionalInfoRequest;
import com.rooming.domain.broker.dto.BrokerProfileData;
import com.rooming.domain.broker.entity.Broker;
import com.rooming.domain.broker.service.BrokerProfileService;
import com.rooming.domain.user.entity.AccountType;
import com.rooming.security.principal.PrincipalDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user/broker/me/additional-info")
public class BrokerAdditionalInfoController {

    private final BrokerProfileService brokerProfileService;

    @PutMapping
    public ResponseEntity<ApiResponse<BrokerProfileData>> updateAdditionalInfo(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @Valid @RequestBody BrokerAdditionalInfoRequest request
    ) {
        Broker broker = requireBroker(principalDetails);
        return ResponseEntity.ok(ApiResponse.success(
                brokerProfileService.updateAdditionalInfo(broker, request),
                "Broker additional information updated."
        ));
    }

    private Broker requireBroker(PrincipalDetails principalDetails) {
        if (principalDetails == null || principalDetails.getUser().getAccountType() != AccountType.BROKER) {
            throw new ForbiddenException("Broker account is required.");
        }
        return (Broker) principalDetails.getUser();
    }
}