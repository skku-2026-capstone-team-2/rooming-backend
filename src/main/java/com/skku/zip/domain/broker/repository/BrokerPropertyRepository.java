package com.skku.zip.domain.broker.repository;

import com.skku.zip.domain.property.entity.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BrokerPropertyRepository extends JpaRepository<Property, Long> {

    @Query("select max(p.propertyId) from Property p")
    Long findMaxPropertyId();
}
