package com.skku.zip.domain.broker.entity;

import com.skku.zip.domain.user.entity.AccountType;
import com.skku.zip.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

@Entity
@Table(name = "brokers")
@Getter
public class Broker implements User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column
    private String provider;

    @Column(name = "login_id", nullable = false, unique = true)
    private String loginId;

    @Column
    private String officeName;

    @Column(unique = true)
    private String registrationNo;

    @Column
    private String officePhone;

    @Column
    private String officeAddress;

    @Column
    private String phoneNumber;

    @Column(nullable = false)
    private boolean isVerified;

    @Column(name = "signed_up_at")
    @CreationTimestamp
    private Timestamp signedUpAt;

    protected Broker() {
    }

    @Builder
    public Broker(String email, String name, String provider, String loginId) {
        this.email = email;
        this.name = name;
        this.provider = provider;
        this.loginId = loginId;
        this.isVerified = false;
    }

    public void updateBrokerProfile(
            String officeName,
            String registrationNo,
            String officePhone,
            String officeAddress,
            String phoneNumber
    ) {
        this.officeName = normalize(officeName);
        this.registrationNo = normalize(registrationNo);
        this.officePhone = normalize(officePhone);
        this.officeAddress = normalize(officeAddress);
        this.phoneNumber = normalize(phoneNumber);
    }

    public boolean isProfileComplete() {
        return isPresent(officeName)
                && isPresent(registrationNo)
                && isPresent(officePhone)
                && isPresent(officeAddress)
                && isPresent(phoneNumber);
    }

    @Override
    public AccountType getAccountType() {
        return AccountType.BROKER;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
