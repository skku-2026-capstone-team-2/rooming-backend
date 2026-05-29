package com.skku.zip.domain.locations.entity.model;

import com.skku.zip.domain.locations.entity.type.INFRA_CATEGORY;
import com.skku.zip.domain.locations.entity.value.RoadAddress;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import java.sql.Timestamp;

@Entity
@Table(
        name = "infrastructures",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_infrastructures_address",
                        columnNames = "address"
                ),
                @UniqueConstraint(
                        name = "uk_infrastructures_location",
                        columnNames = "location"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Infrastructure {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column
    private INFRA_CATEGORY category;

    @Column(nullable = false, columnDefinition = "geography(Point,4326)")
    private Point location;

    @Column
    private RoadAddress address;

    @CreationTimestamp
    @Column(name = "created_at")
    private Timestamp createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Timestamp updatedAt;

    public Infrastructure(String name, INFRA_CATEGORY category, double latitude, double longitude, RoadAddress address) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Infrastructure name must not be blank.");
        }
        if (category == null) {
            throw new IllegalArgumentException("Infrastructure category must not be null.");
        }

        this.name = name.trim();
        this.category = category;
        this.location = createPoint(latitude, longitude);
        this.address = address;
    }

    public double getLatitude() {
        return location.getY();
    }

    public double getLongitude() {
        return location.getX();
    }

    private Point createPoint(double latitude, double longitude) {
        validateLatitude(latitude);
        validateLongitude(longitude);

        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
        point.setSRID(4326);
        return point;
    }

    private void validateLatitude(double latitude) {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90.");
        }
    }

    private void validateLongitude(double longitude) {
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180.");
        }
    }
}
