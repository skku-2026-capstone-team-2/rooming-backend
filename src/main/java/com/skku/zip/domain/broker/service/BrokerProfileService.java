package com.skku.zip.domain.broker.service;

import com.skku.zip.domain.broker.dto.BrokerProfileData;
import com.skku.zip.domain.broker.dto.BrokerProfileUpdateRequest;
import com.skku.zip.domain.broker.entity.Broker;
import com.skku.zip.domain.broker.repository.BrokerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BrokerProfileService {

    private final BrokerRepository brokerRepository;

    @Transactional(readOnly = true)
    public BrokerProfileData toProfile(Broker broker) {
        return toData(broker);
    }

    @Transactional
    public BrokerProfileData updateProfile(Broker broker, BrokerProfileUpdateRequest request) {
        Broker managedBroker = brokerRepository.findById(broker.getId())
                .orElseThrow(() -> new IllegalArgumentException("Broker not found."));

        managedBroker.updateBrokerProfile(
                request.officeName(),
                request.registrationNo(),
                request.officePhone(),
                request.officeAddress(),
                request.phoneNumber()
        );

        return toData(managedBroker);
    }

    private BrokerProfileData toData(Broker broker) {
        return new BrokerProfileData(
                broker.getId(),
                broker.getEmail(),
                broker.getName(),
                broker.getAccountType().name(),
                broker.getOfficeName(),
                broker.getRegistrationNo(),
                broker.getOfficePhone(),
                broker.getOfficeAddress(),
                broker.getPhoneNumber(),
                broker.isVerified(),
                broker.isProfileComplete()
        );
    }
}
