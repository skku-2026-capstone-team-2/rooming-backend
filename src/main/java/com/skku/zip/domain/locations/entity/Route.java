package com.skku.zip.domain.locations.entity;

import jakarta.persistence.*;

import java.sql.Timestamp;

@Entity
@Table(name = "routes")
public class Route {

    @EmbeddedId
    private UserplacePropertyId userplacePropertyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userplaceId")
    @JoinColumn(name = "userplace_id", nullable = false)
    private UserPlace userPlace;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("infrastructureId")
    @JoinColumn(name = "infrastructure_id", nullable = false)
    private Infrastructure infrastructure;

    @Enumerated(EnumType.STRING)
    private TRANSPORT_MODE transport_mode;

    private Minutes duration_minutes;

    @Column(name = "route_json", columnDefinition = "jsonb")
    private String routeJson;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @Column(name = "updated_at", nullable = false)
    private Timestamp updatedAt;
}
