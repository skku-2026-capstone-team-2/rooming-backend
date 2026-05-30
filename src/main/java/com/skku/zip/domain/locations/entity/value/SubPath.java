package com.skku.zip.domain.locations.entity.value;


import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class SubPath {
    private int trafficType;
    private Minutes sectionTime;
    private String startName;
    private String endName;
    private String laneName;
    private Integer distanceMeters;
    private String description;
    private List<RoutePoint> points;

    public SubPath(int trafficType, Minutes sectionTime, String startName, String endName, String laneName) {
        this(trafficType, sectionTime, startName, endName, laneName, null, null, List.of());
    }

    public SubPath(
            int trafficType,
            Minutes sectionTime,
            String startName,
            String endName,
            String laneName,
            Integer distanceMeters,
            String description,
            List<RoutePoint> points
    ) {
        this.trafficType = trafficType;
        this.sectionTime = sectionTime;
        this.startName = startName;
        this.endName = endName;
        this.laneName = laneName;
        this.distanceMeters = distanceMeters;
        this.description = description;
        this.points = points == null ? List.of() : points;
    }
}
