package com.rooming.domain.broker.repository;

import com.rooming.domain.property.entity.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BrokerPropertyRepository extends JpaRepository<Property, Long> {

    @Query("select max(p.propertyId) from Property p")
    Long findMaxPropertyId();
}