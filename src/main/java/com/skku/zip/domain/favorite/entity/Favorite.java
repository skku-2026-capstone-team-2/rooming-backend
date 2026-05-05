package com.skku.zip.domain.favorite.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.skku.zip.common.exception.BadRequestException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.sql.Timestamp;

@Entity
@Table(
        name = "favorites",
        uniqueConstraints = @UniqueConstraint(name = "uk_favorites_property_id", columnNames = "property_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column
    private String title;

    @Column(name = "road_address")
    private String roadAddress;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "snapshot_json", nullable = false, columnDefinition = "jsonb")
    private JsonNode snapshotJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Timestamp updatedAt;

    public Favorite(JsonNode snapshotJson) {
        if (snapshotJson == null || snapshotJson.isNull()) {
            throw new BadRequestException("Favorite snapshot must not be null.");
        }

        this.propertyId = requiredLong(snapshotJson, "propertyId");
        this.title = text(snapshotJson, "title");
        this.roadAddress = text(snapshotJson, "roadAddress");
        this.snapshotJson = snapshotJson;
    }

    private Long requiredLong(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (!value.canConvertToLong()) {
            throw new BadRequestException(fieldName + " must be provided in favorite snapshot.");
        }
        return value.asLong();
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() || value.asText().isBlank()
                ? null
                : value.asText();
    }
}
