package com.skku.zip.domain.broker.controller;

import com.skku.zip.common.dto.ApiResponse;
import com.skku.zip.common.exception.ForbiddenException;
import com.skku.zip.domain.broker.dto.BrokerOfficeCreateRequest;
import com.skku.zip.domain.broker.dto.BrokerOfficeData;
import com.skku.zip.domain.broker.dto.BrokerOfficeListData;
import com.skku.zip.domain.broker.service.BrokerOfficeService;
import com.skku.zip.domain.user.entity.AccountType;
import com.skku.zip.security.principal.PrincipalDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/broker-offices")
public class BrokerOfficeController {

    private final BrokerOfficeService brokerOfficeService;

    @GetMapping
    public ResponseEntity<ApiResponse<BrokerOfficeListData>> getOffices(
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        requireBroker(principalDetails);
        return ResponseEntity.ok(ApiResponse.success(
                brokerOfficeService.getOffices(),
                "Broker office list fetched."
        ));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BrokerOfficeData>> createOffice(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @Valid @RequestBody BrokerOfficeCreateRequest request
    ) {
        requireBroker(principalDetails);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                brokerOfficeService.createOffice(request),
                "Broker office saved."
        ));
    }

    private void requireBroker(PrincipalDetails principalDetails) {
        if (principalDetails == null || principalDetails.getUser().getAccountType() != AccountType.BROKER) {
            throw new ForbiddenException("Broker account is required.");
        }
    }
}
