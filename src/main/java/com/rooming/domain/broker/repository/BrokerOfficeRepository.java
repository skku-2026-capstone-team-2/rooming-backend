package com.rooming.domain.broker.repository;

import com.rooming.domain.broker.entity.BrokerOffice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BrokerOfficeRepository extends JpaRepository<BrokerOffice, Long> {
    Optional<BrokerOffice> findByOfficeNameAndOfficePhoneAndOfficeAddress(
            String officeName,
            String officePhone,
            String officeAddress
    );
}