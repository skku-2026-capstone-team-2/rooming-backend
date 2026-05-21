package com.skku.zip.domain.broker.entity;

import com.skku.zip.domain.property.entity.Property;
import com.skku.zip.domain.user.entity.AccountType;
import com.skku.zip.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;
import java.util.LinkedHashSet;
import java.util.Set;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "broker_office_id")
    private BrokerOffice office;

    @Column(unique = true)
    private String registrationNo;

    @Column
    private String phoneNumber;

    @Column(name = "verification_document_file_name")
    private String verificationDocumentFileName;

    @Column(name = "verification_document_content_type")
    private String verificationDocumentContentType;

    @Column(name = "verification_document", columnDefinition = "bytea")
    private byte[] verificationDocument;

    @OneToMany(mappedBy = "broker", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Property> properties = new LinkedHashSet<>();

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

    public void updateAdditionalInfo(
            BrokerOffice office,
            String registrationNo,
            String phoneNumber,
            String verificationDocumentFileName,
            String verificationDocumentContentType,
            byte[] verificationDocument
    ) {
        String normalizedRegistrationNo = normalize(registrationNo);
        String normalizedPhoneNumber = normalize(phoneNumber);
        if (!isPresent(normalizedRegistrationNo) || !isPresent(normalizedPhoneNumber)) {
            throw new IllegalArgumentException("Registration number and phone number are required.");
        }
        if (verificationDocument == null || verificationDocument.length == 0) {
            throw new IllegalArgumentException("Verification document is required.");
        }

        this.office = office;
        this.registrationNo = normalizedRegistrationNo;
        this.phoneNumber = normalizedPhoneNumber;
        this.verificationDocumentFileName = normalize(verificationDocumentFileName);
        this.verificationDocumentContentType = normalize(verificationDocumentContentType);
        this.verificationDocument = verificationDocument.clone();
    }

    public boolean isProfileComplete() {
        return isPresent(registrationNo)
                && isPresent(phoneNumber)
                && hasVerificationDocument();
    }

    public boolean hasVerificationDocument() {
        return verificationDocument != null && verificationDocument.length > 0;
    }

    public void addProperty(Property property) {
        if (property == null) {
            throw new IllegalArgumentException("Property must not be null.");
        }
        properties.add(property);
        property.assignBroker(this);
    }

    public String getOfficeName() {
        return office == null ? null : office.getOfficeName();
    }

    public String getOfficePhone() {
        return office == null ? null : office.getOfficePhone();
    }

    public String getOfficeAddress() {
        return office == null ? null : office.getOfficeAddress();
    }

    public Long getOfficeId() {
        return office == null ? null : office.getId();
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
