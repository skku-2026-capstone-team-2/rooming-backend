package com.skku.zip.domain.locations.service;

import com.skku.zip.domain.locations.dto.RouteGeometryDetail;
import com.skku.zip.domain.locations.entity.value.Minutes;
import com.skku.zip.domain.locations.entity.value.Path;
import com.skku.zip.domain.locations.entity.value.RoutePoint;
import com.skku.zip.domain.locations.entity.value.SubPath;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class RouteGeometryServiceTest {

    private final RouteGeometryService routeGeometryService = new RouteGeometryService();

    @Test
    void routeGeometryProjectionKeepsReturnedPointsWithinDetailBudgets() {
        List<RoutePoint> originalPoints = IntStream.range(0, 600)
                .mapToObj(index -> new RoutePoint(
                        37.2945 + index * 0.00001,
                        126.9748 + Math.sin(index / 3.0) * 0.0002
                ))
                .toList();
        Path original = new Path(
                new Minutes(18),
                1,
                List.of(new SubPath(2, new Minutes(18), "Start", "End", "62-1", 5_000, null, originalPoints))
        );

        Path summary = routeGeometryService.project(original, RouteGeometryDetail.SUMMARY);
        Path detail = routeGeometryService.project(original, RouteGeometryDetail.DETAIL);

        assertThat(routeGeometryService.pointCount(summary)).isLessThanOrEqualTo(100);
        assertThat(routeGeometryService.pointCount(detail)).isLessThanOrEqualTo(300);
        assertThat(detail.getSubPaths().getFirst().getPoints().getFirst()).usingRecursiveComparison()
                .isEqualTo(originalPoints.getFirst());
        assertThat(detail.getSubPaths().getFirst().getPoints().getLast()).usingRecursiveComparison()
                .isEqualTo(originalPoints.getLast());
        assertThat(original.getSubPaths().getFirst().getPoints()).hasSize(600);
    }
}
