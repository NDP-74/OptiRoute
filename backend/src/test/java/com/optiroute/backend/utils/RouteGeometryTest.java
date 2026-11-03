package com.optiroute.backend.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.optiroute.backend.model.Position;

class RouteGeometryTest {

    @Test
    void shouldMeasureDistanceToSegmentInteriorRatherThanItsVertices() {
        Position parking = new Position(0.1, 0);
        Position from = new Position(0, -2);
        Position to = new Position(0, 2);

        var projection = RouteGeometry.project(parking, from, to);

        assertEquals(11_119.49, projection.distanceMeters(), 0.01);
        assertEquals(0.5, projection.fraction(), 1e-9);
        assertTrue(RouteGeometry.distance(parking, from) > 200_000);
        assertTrue(RouteGeometry.distance(parking, to) > 200_000);
    }

    @Test
    void shouldClampToEndpointsOutsideTheSegmentAndHandleRepeatedCoordinates() {
        var projection = RouteGeometry.project(new Position(0, 3), new Position(0, 0), new Position(0, 1));
        assertEquals(1, projection.fraction());
        assertEquals(222_389.85, projection.distanceMeters(), 0.01);
        var repeated = RouteGeometry.project(new Position(1, 0), new Position(0, 0), new Position(0, 0));
        assertEquals(111_194.93, repeated.distanceMeters(), 0.01);
    }

    @Test
    void shouldWorkAcrossTheAntimeridianAndAtHighLatitudes() {
        var crossing = RouteGeometry.project(new Position(0.1, 180), new Position(0, 179), new Position(0, -179));
        assertEquals(11_119.49, crossing.distanceMeters(), 0.01);
        assertEquals(0.5, crossing.fraction(), 1e-9);
        var polar = RouteGeometry.project(new Position(89, 0), new Position(88, -90), new Position(88, 90));
        assertEquals(111_194.93, polar.distanceMeters(), 0.01);
    }

    @Test
    void shouldResolveTheFiftyKilometerThresholdInMeters() {
        Position from = new Position(0, -1);
        Position to = new Position(0, 1);
        for (double meters : new double[] {49_999, 50_000, 50_001}) {
            double latitude = Math.toDegrees(meters / 6_371_000);
            assertEquals(meters, RouteGeometry.project(new Position(latitude, 0), from, to).distanceMeters(), 1e-7);
        }
    }
}
