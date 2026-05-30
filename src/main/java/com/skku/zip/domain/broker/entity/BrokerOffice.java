package com.skku.zip.domain.broker.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "broker_offices",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_broker_offices_details",
                columnNames = {"office_name", "office_phone", "office_address"}
        )
)
@Getter
@NoArgsConstructor
public class BrokerOffice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "office_name", nullable = false)
    private String officeName;

    @Column(name = "office_phone", nullable = false)
    private String officePhone;

    @Column(name = "office_address", nullable = false)
    private String officeAddress;

    public BrokerOffice(String officeName, String officePhone, String officeAddress) {
        this.officeName = normalize(officeName);
        this.officePhone = normalize(officePhone);
        this.officeAddress = normalize(officeAddress);
        if (!isComplete()) {
            throw new IllegalArgumentException("Broker office name, phone number, and address are required.");
        }
    }

    public boolean isComplete() {
        return isPresent(officeName)
                && isPresent(officePhone)
                && isPresent(officeAddress);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
