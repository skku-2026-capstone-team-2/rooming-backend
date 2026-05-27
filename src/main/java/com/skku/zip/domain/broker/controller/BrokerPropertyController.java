package com.skku.zip.domain.broker.controller;

import com.skku.zip.common.dto.ApiResponse;
import com.skku.zip.common.exception.ForbiddenException;
import com.skku.zip.domain.broker.dto.BrokerPropertyCreateRequest;
import com.skku.zip.domain.broker.dto.BrokerPropertyData;
import com.skku.zip.domain.broker.service.BrokerPropertyService;
import com.skku.zip.domain.user.entity.AccountType;
import com.skku.zip.domain.broker.entity.Broker;
import com.skku.zip.security.principal.PrincipalDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user/broker/me/properties")
public class BrokerPropertyController {

    private final BrokerPropertyService brokerPropertyService;

    @PostMapping
    public ResponseEntity<ApiResponse<BrokerPropertyData>> createProperty(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @Valid @RequestBody BrokerPropertyCreateRequest request
    ) {
        Broker broker = requireBroker(principalDetails);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                brokerPropertyService.createProperty(broker, request),
                "Property saved."
        ));
    }

    private Broker requireBroker(PrincipalDetails principalDetails) {
        if (principalDetails == null || principalDetails.getUser().getAccountType() != AccountType.BROKER) {
            throw new ForbiddenException("Broker account is required.");
        }
        return (Broker) principalDetails.getUser();
    }
}
