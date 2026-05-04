package com.skku.zip.domain.locations.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "user_place")
@Getter
@Setter
public class UserPlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long  id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private PLACE_CATEGORY category;

    @Column(nullable = false, columnDefinition = "geography(Point,4326)")
    private Point location;

    private RoadAddress address;
}
