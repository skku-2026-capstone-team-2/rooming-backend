package com.rooming.domain.broker.repository;

import com.rooming.domain.broker.dto.BrokerPropertySummaryData;
import com.rooming.domain.property.entity.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BrokerPropertyRepository extends JpaRepository<Property, Long> {

    @Query("select max(p.propertyId) from Property p")
    Long findMaxPropertyId();

    @Query("""
            select new com.rooming.domain.broker.dto.BrokerPropertySummaryData(p.propertyId, p.title)
            from Property p
            where p.broker.id = :brokerId
            order by p.propertyId desc
            """)
    List<BrokerPropertySummaryData> findSummariesByBrokerId(@Param("brokerId") Long brokerId);
}
