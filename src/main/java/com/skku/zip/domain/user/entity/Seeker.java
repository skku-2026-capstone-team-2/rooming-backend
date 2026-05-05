package com.skku.zip.domain.user.entity;

import com.skku.zip.domain.favorite.entity.Favorite;
import com.skku.zip.domain.locations.entity.model.Userplace;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;
import java.util.List;

@Entity
@Table(name = "seekers")
@Getter
public class Seeker implements User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column
    private String provider;

    @Column(name = "login_id", nullable = false, unique = true)
    private String loginId;

    @ManyToOne
    @JoinColumn(name = "userplace_id")
    private Userplace userplace;

    @Column(name = "signed_up_at")
    @CreationTimestamp
    private Timestamp signedUpAt;

    protected Seeker() {
    }

    @Builder
    public Seeker(String name, String email, String provider, String loginId) {
        this.name = name;
        this.email = email;
        this.provider = provider;
        this.loginId = loginId;
    }

    @Override
    public AccountType getAccountType() {
        return AccountType.SEEKER;
    }
}
