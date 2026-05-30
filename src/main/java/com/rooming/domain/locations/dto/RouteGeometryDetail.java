package com.rooming.domain.locations.dto;

public enum RouteGeometryDetail {
    SUMMARY(100, 8.0),
    DETAIL(300, 2.0);

    private final int maxPoints;
    private final double toleranceMeters;

    RouteGeometryDetail(int maxPoints, double toleranceMeters) {
        this.maxPoints = maxPoints;
        this.toleranceMeters = toleranceMeters;
    }

    public int maxPoints() {
        return maxPoints;
    }

    public double toleranceMeters() {
        return toleranceMeters;
    }
}