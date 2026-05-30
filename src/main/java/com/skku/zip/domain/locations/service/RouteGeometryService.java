package com.skku.zip.domain.locations.service;

import com.skku.zip.domain.locations.dto.RouteGeometryDetail;
import com.skku.zip.domain.locations.entity.value.Path;
import com.skku.zip.domain.locations.entity.value.RoutePoint;
import com.skku.zip.domain.locations.entity.value.SubPath;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class RouteGeometryService {

    private static final double METERS_PER_LATITUDE_DEGREE = 110_540.0;
    private static final double METERS_PER_LONGITUDE_DEGREE = 111_320.0;

    public Path project(Path path, RouteGeometryDetail detail) {
        if (path == null) {
            return null;
        }

        List<SubPath> subPaths = path.getSubPaths() == null
                ? List.of()
                : path.getSubPaths().stream()
                .map(subPath -> copyWithPoints(
                        subPath,
                        simplify(subPath.getPoints(), detail.toleranceMeters())
                ))
                .toList();

        return new Path(
                path.getTotalTime(),
                path.getTransferCount(),
                fitPointBudget(subPaths, detail.maxPoints())
        );
    }

    public int pointCount(Path path) {
        if (path == null || path.getSubPaths() == null) {
            return 0;
        }

        return path.getSubPaths().stream()
                .map(SubPath::getPoints)
                .filter(points -> points != null)
                .mapToInt(List::size)
                .sum();
    }

    private List<SubPath> fitPointBudget(List<SubPath> subPaths, int maxPoints) {
        if (pointCount(new Path(null, 0, subPaths)) <= maxPoints) {
            return subPaths;
        }

        List<Integer> allocations = allocatePointBudget(subPaths, maxPoints);
        List<SubPath> constrained = new ArrayList<>();
        for (int index = 0; index < subPaths.size(); index++) {
            SubPath subPath = subPaths.get(index);
            constrained.add(copyWithPoints(
                    subPath,
                    sample(subPath.getPoints(), allocations.get(index))
            ));
        }
        return constrained;
    }

    private List<Integer> allocatePointBudget(List<SubPath> subPaths, int maxPoints) {
        List<Integer> allocations = new ArrayList<>(Collections.nCopies(subPaths.size(), 0));
        int allocated = 0;

        for (int index = 0; index < subPaths.size(); index++) {
            int pointCount = points(subPaths.get(index)).size();
            int minimum = pointCount == 0 ? 0 : Math.min(pointCount, 2);
            allocations.set(index, minimum);
            allocated += minimum;
        }

        if (allocated > maxPoints) {
            int remaining = maxPoints;
            for (int index = 0; index < subPaths.size(); index++) {
                int allocation = remaining > 0 && !points(subPaths.get(index)).isEmpty() ? 1 : 0;
                allocations.set(index, allocation);
                remaining -= allocation;
            }
            return allocations;
        }

        while (allocated < maxPoints) {
            int remainingCapacity = 0;
            for (int index = 0; index < subPaths.size(); index++) {
                remainingCapacity += points(subPaths.get(index)).size() - allocations.get(index);
            }
            if (remainingCapacity == 0) {
                return allocations;
            }

            boolean progressed = false;
            for (int index = 0; index < subPaths.size() && allocated < maxPoints; index++) {
                int capacity = points(subPaths.get(index)).size() - allocations.get(index);
                if (capacity <= 0) {
                    continue;
                }
                allocations.set(index, allocations.get(index) + 1);
                allocated++;
                progressed = true;
            }
            if (!progressed) {
                return allocations;
            }
        }

        return allocations;
    }

    private List<RoutePoint> simplify(List<RoutePoint> routePoints, double toleranceMeters) {
        List<RoutePoint> points = points(routePoints);
        if (points.size() <= 2) {
            return points;
        }

        boolean[] keep = new boolean[points.size()];
        keep[0] = true;
        keep[points.size() - 1] = true;
        markDouglasPeucker(points, 0, points.size() - 1, toleranceMeters, keep);

        List<RoutePoint> simplified = new ArrayList<>();
        for (int index = 0; index < points.size(); index++) {
            if (keep[index]) {
                simplified.add(points.get(index));
            }
        }
        return simplified;
    }

    private void markDouglasPeucker(
            List<RoutePoint> points,
            int startIndex,
            int endIndex,
            double toleranceMeters,
            boolean[] keep
    ) {
        if (endIndex <= startIndex + 1) {
            return;
        }

        double farthestDistance = -1;
        int farthestIndex = -1;
        RoutePoint start = points.get(startIndex);
        RoutePoint end = points.get(endIndex);
        for (int index = startIndex + 1; index < endIndex; index++) {
            double distance = perpendicularDistanceMeters(points.get(index), start, end);
            if (distance > farthestDistance) {
                farthestDistance = distance;
                farthestIndex = index;
            }
        }

        if (farthestIndex < 0 || farthestDistance <= toleranceMeters) {
            return;
        }

        keep[farthestIndex] = true;
        markDouglasPeucker(points, startIndex, farthestIndex, toleranceMeters, keep);
        markDouglasPeucker(points, farthestIndex, endIndex, toleranceMeters, keep);
    }

    private double perpendicularDistanceMeters(RoutePoint point, RoutePoint start, RoutePoint end) {
        double averageLatitude = Math.toRadians((start.getLatitude() + end.getLatitude()) / 2);
        double startX = longitudeMeters(start.getLongitude(), averageLatitude);
        double startY = latitudeMeters(start.getLatitude());
        double endX = longitudeMeters(end.getLongitude(), averageLatitude);
        double endY = latitudeMeters(end.getLatitude());
        double pointX = longitudeMeters(point.getLongitude(), averageLatitude);
        double pointY = latitudeMeters(point.getLatitude());

        double deltaX = endX - startX;
        double deltaY = endY - startY;
        if (deltaX == 0 && deltaY == 0) {
            return Math.hypot(pointX - startX, pointY - startY);
        }

        double numerator = Math.abs(deltaY * pointX - deltaX * pointY + endX * startY - endY * startX);
        return numerator / Math.hypot(deltaX, deltaY);
    }

    private double longitudeMeters(double longitude, double latitudeRadians) {
        return longitude * METERS_PER_LONGITUDE_DEGREE * Math.cos(latitudeRadians);
    }

    private double latitudeMeters(double latitude) {
        return latitude * METERS_PER_LATITUDE_DEGREE;
    }

    private List<RoutePoint> sample(List<RoutePoint> routePoints, int targetCount) {
        List<RoutePoint> points = points(routePoints);
        if (targetCount <= 0 || points.isEmpty()) {
            return List.of();
        }
        if (targetCount >= points.size()) {
            return points;
        }
        if (targetCount == 1) {
            return List.of(points.getFirst());
        }

        List<RoutePoint> sampled = new ArrayList<>();
        for (int index = 0; index < targetCount; index++) {
            int pointIndex = (int) Math.round((double) index * (points.size() - 1) / (targetCount - 1));
            sampled.add(points.get(pointIndex));
        }
        return sampled;
    }

    private SubPath copyWithPoints(SubPath subPath, List<RoutePoint> routePoints) {
        return new SubPath(
                subPath.getTrafficType(),
                subPath.getSectionTime(),
                subPath.getStartName(),
                subPath.getEndName(),
                subPath.getLaneName(),
                subPath.getDistanceMeters(),
                subPath.getDescription(),
                routePoints
        );
    }

    private List<RoutePoint> points(SubPath subPath) {
        return points(subPath.getPoints());
    }

    private List<RoutePoint> points(List<RoutePoint> routePoints) {
        return routePoints == null ? List.of() : List.copyOf(routePoints);
    }
}
