package com.rooming.domain.locations.entity.model;

import com.rooming.domain.locations.entity.type.PLACE_CATEGORY;
import com.rooming.domain.locations.entity.value.RoadAddress;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

@Entity
@Table(
        name = "target_places",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_target_places_address",
                columnNames = "address"
        )
)
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class TargetPlace {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PLACE_CATEGORY category = PLACE_CATEGORY.ETC;

    @Column(nullable = false, columnDefinition = "geography(Point,4326)")
    private Point location;

    @Column
    private RoadAddress address;

    public TargetPlace(
            PLACE_CATEGORY category,
            String name,
            RoadAddress address,
            double latitude,
            double longitude
    ) {
        if (category == null) {
            throw new IllegalArgumentException("Target place category must not be null.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Target place name must not be blank.");
        }

        this.name = name.trim();
        this.category = category;
        this.address = address;
        this.location = createPoint(latitude, longitude);
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