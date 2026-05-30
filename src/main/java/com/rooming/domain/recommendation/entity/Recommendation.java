package com.rooming.domain.recommendation.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.rooming.domain.property.entity.Property;
import com.rooming.domain.seeker.entity.Seeker;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.sql.Timestamp;
import java.time.Instant;

@Entity
@Table(name = "recommendations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seeker_id", nullable = false)
    private Seeker seeker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "snapshot_json", nullable = false, columnDefinition = "jsonb")
    private JsonNode snapshotJson;

    @Column(nullable = false)
    private boolean favorite;

    @Column(name = "favorited_at")
    private Timestamp favoritedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Timestamp updatedAt;

    public Recommendation(
            Seeker seeker,
            Property property,
            JsonNode snapshotJson
    ) {
        if (seeker == null || seeker.getId() == null) {
            throw new IllegalArgumentException("Recommendation seeker must be saved.");
        }
        if (property == null || property.getPropertyId() == null) {
            throw new IllegalArgumentException("Recommendation property must be saved.");
        }
        if (snapshotJson == null || snapshotJson.isNull()) {
            throw new IllegalArgumentException("Recommendation snapshot must be provided.");
        }

        this.seeker = seeker;
        this.property = property;
        this.snapshotJson = snapshotJson;
    }

    public void markFavorite() {
        if (favorite) {
            return;
        }
        favorite = true;
        favoritedAt = Timestamp.from(Instant.now());
    }

    public void removeFavorite() {
        favorite = false;
        favoritedAt = null;
    }
}