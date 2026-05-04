package com.skku.zip.domain.user.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

@Entity
@Getter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column
    private String provider;

    @Column(name = "login_id")
    private String loginId;

    @Column(name = "signedUpAt")
    @CreationTimestamp
    private Timestamp signedUpAt;

    @Column
    private boolean isQualified;

    public User() {
    }

    @Builder
    public User(String name, String email, String provider, String loginId) {
        this.name = name;
        this.email = email;
        this.provider = provider;
        this.loginId = loginId;
        isQualified = false;
    }

    public void updateQualification(boolean isQualified) {
        this.isQualified = isQualified;
    }
}
