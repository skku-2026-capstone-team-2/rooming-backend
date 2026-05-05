package com.skku.zip.domain.broker.repository;

import com.skku.zip.domain.broker.entity.Broker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BrokerRepository extends JpaRepository<Broker, Long> {
    Optional<Broker> findByLoginId(String loginId);
}
