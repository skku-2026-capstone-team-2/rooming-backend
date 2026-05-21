package com.skku.zip.domain.broker.controller;

import com.skku.zip.common.dto.ApiResponse;
import com.skku.zip.domain.broker.dto.BrokerAdditionalInfoRequest;
import com.skku.zip.domain.broker.dto.BrokerProfileData;
import com.skku.zip.domain.broker.entity.Broker;
import com.skku.zip.domain.broker.service.BrokerProfileService;
import com.skku.zip.domain.user.entity.AccountType;
import com.skku.zip.security.principal.PrincipalDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user/broker/me/additional-info")
public class BrokerAdditionalInfoController {

    private final BrokerProfileService brokerProfileService;

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<BrokerProfileData> updateAdditionalInfo(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @Valid @RequestPart("additionalInfo") BrokerAdditionalInfoRequest request,
            @RequestPart("verificationDocument") MultipartFile verificationDocument
    ) {
        Broker broker = requireBroker(principalDetails);
        return ApiResponse.success(
                brokerProfileService.updateAdditionalInfo(broker, request, verificationDocument),
                "Broker additional information updated."
        );
    }

    private Broker requireBroker(PrincipalDetails principalDetails) {
        if (principalDetails == null || principalDetails.getUser().getAccountType() != AccountType.BROKER) {
            throw new AccessDeniedException("Broker account is required.");
        }
        return (Broker) principalDetails.getUser();
    }
}
