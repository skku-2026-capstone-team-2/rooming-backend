package com.skku.zip.domain.seeker.entity;

import com.skku.zip.domain.locations.entity.model.TargetPlace;
import com.skku.zip.domain.user.entity.AccountType;
import com.skku.zip.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

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

    @OneToMany(mappedBy = "seeker", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RegisteredTargetPlace> targetPlaces = new LinkedHashSet<>();

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

    public RegisteredTargetPlace addTargetPlace(TargetPlace targetPlace, String memo) {
        if (targetPlace == null) {
            throw new IllegalArgumentException("Target place must not be null.");
        }

        return findTargetPlace(targetPlace.getId())
                .map(existing -> {
                    existing.updateMemo(memo);
                    return existing;
                })
                .orElseGet(() -> {
                    RegisteredTargetPlace registeredTargetPlace = new RegisteredTargetPlace(this, targetPlace, memo);
                    targetPlaces.add(registeredTargetPlace);
                    return registeredTargetPlace;
                });
    }

    public RegisteredTargetPlace replaceTargetPlace(
            RegisteredTargetPlace currentTargetPlace,
            TargetPlace targetPlace,
            String memo
    ) {
        if (currentTargetPlace == null) {
            throw new IllegalArgumentException("Current target place must not be null.");
        }
        if (targetPlace == null) {
            throw new IllegalArgumentException("Target target place must not be null.");
        }

        if (currentTargetPlace.hasTargetPlaceId(targetPlace.getId())) {
            currentTargetPlace.updateMemo(memo);
            return currentTargetPlace;
        }

        removeTargetPlace(currentTargetPlace);
        return addTargetPlace(targetPlace, memo);
    }

    public void removeTargetPlace(RegisteredTargetPlace targetPlace) {
        targetPlaces.remove(targetPlace);
    }

    private Optional<RegisteredTargetPlace> findTargetPlace(Long targetPlaceId) {
        return targetPlaces.stream()
                .filter(targetPlace -> targetPlace.hasTargetPlaceId(targetPlaceId))
                .findFirst();
    }

    @Override
    public AccountType getAccountType() {
        return AccountType.SEEKER;
    }
}
