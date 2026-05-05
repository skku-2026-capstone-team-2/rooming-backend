package com.skku.zip.domain.locations.entity.model;

import com.skku.zip.domain.locations.entity.type.PLACE_CATEGORY;
import com.skku.zip.domain.locations.entity.type.USER_PLACE_TYPE;
import com.skku.zip.domain.locations.entity.value.RoadAddress;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

@Entity
@Table(name = "user_places")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Userplace {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private USER_PLACE_TYPE placeType = USER_PLACE_TYPE.ETC;

    @Enumerated(EnumType.STRING)
    private PLACE_CATEGORY category;

    @Column(nullable = false, columnDefinition = "geography(Point,4326)")
    private Point location;

    @Column
    private RoadAddress address;

    @Column
    private String memo;

    @Column(nullable = false)
    private boolean active = true;

    public Userplace(double latitude, double longitude) {
        this.location = createPoint(latitude, longitude);
    }

    public Userplace(double latitude, double longitude, String name, PLACE_CATEGORY category, RoadAddress address) {
        this(latitude, longitude);
        updatePlaceInfo(name, category, address);
    }

    public Userplace(
            USER_PLACE_TYPE placeType,
            String name,
            RoadAddress address,
            double latitude,
            double longitude,
            String memo,
            boolean active
    ) {
        this(latitude, longitude);
        updateUserPlace(placeType, name, address, latitude, longitude, memo, active);
    }

    public void updatePlaceInfo(String name, PLACE_CATEGORY category, RoadAddress address) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("User place name must not be blank.");
        }
        this.name = name.trim();
        this.category = category == null ? PLACE_CATEGORY.COMPANY : category;
        this.placeType = mapPlaceType(this.category);
        this.address = address;
    }

    public void updateUserPlace(
            USER_PLACE_TYPE placeType,
            String name,
            RoadAddress address,
            double latitude,
            double longitude,
            String memo,
            boolean active
    ) {
        if (placeType == null) {
            throw new IllegalArgumentException("User place type must not be null.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("User place name must not be blank.");
        }

        this.placeType = placeType;
        this.name = name.trim();
        this.category = mapCategory(placeType);
        this.address = address;
        this.location = createPoint(latitude, longitude);
        this.memo = normalizeMemo(memo);
        this.active = active;
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

    private PLACE_CATEGORY mapCategory(USER_PLACE_TYPE placeType) {
        return switch (placeType) {
            case UNIVERSITY -> PLACE_CATEGORY.SCHOOL;
            case COMPANY, PART_TIME, HOME, ETC -> PLACE_CATEGORY.COMPANY;
        };
    }

    private USER_PLACE_TYPE mapPlaceType(PLACE_CATEGORY category) {
        return switch (category) {
            case SCHOOL -> USER_PLACE_TYPE.UNIVERSITY;
            case COMPANY, SUBWAY_STATION, BUS_TERMINAL -> USER_PLACE_TYPE.ETC;
        };
    }

    private String normalizeMemo(String memo) {
        return memo == null || memo.isBlank() ? null : memo.trim();
    }

}
