package com.optiroute.backend.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class RouteTimelineTest {

    @Test
    void shouldFindNearestSectionAndKeepEveryWaypointOnOneSideOfTheParkingDetour() {
        RouteTimeline timeline = new RouteTimeline(List.of(
            new RouteTimeline.Portion(List.of(new Position(0, 0), new Position(0, 2)), 100, 0),
            new RouteTimeline.Portion(List.of(new Position(0, 2), new Position(0, 4)), 100, 1)
        ), 200);
        var middle = timeline.proximityTo(new Position(0.1, 3));
        assertEquals(11_119.49, middle.distanceMeters(), 0.01);
        assertEquals(1, middle.firstWaypointCount());
        assertEquals(1, middle.remainingWaypointIndex());
        var waypoint = timeline.proximityTo(new Position(0.1, 2));
        assertEquals(1, waypoint.firstWaypointCount());
        assertEquals(1, waypoint.remainingWaypointIndex());
        assertEquals(0, timeline.proximityTo(new Position(0, 0)).firstWaypointCount());
        assertThrows(IllegalStateException.class, () -> new RouteTimeline(List.of(), 0).proximityTo(new Position(0, 0)));
    }

    @Test
    void shouldUseIndividualDurationsAndFollowBentGeometryRatherThanEndpointChord() {
        RouteTimeline timeline = new RouteTimeline(List.of(
            new RouteTimeline.Portion(List.of(new Position(0, 0), new Position(0, 1)), 90, 0),
            new RouteTimeline.Portion(List.of(new Position(0, 1), new Position(1, 1), new Position(1, 2)), 10, 0)
        ), 100);

        Position stop = timeline.positionAt(95).position();
        assertEquals(0.99992, stop.getLat(), 0.0001);
        assertEquals(1, stop.getLng(), 0.000001);
        assertEquals(0, timeline.positionAt(95).firstWaypointCount());
    }

    @Test
    void shouldDistributeWaypointsOnEachSideAndConsumeWaypointAtExactCut() {
        RouteTimeline timeline = new RouteTimeline(List.of(
            new RouteTimeline.Portion(List.of(new Position(0, 0), new Position(0, 1)), 100, 0),
            new RouteTimeline.Portion(List.of(new Position(0, 1), new Position(0, 2)), 100, 1),
            new RouteTimeline.Portion(List.of(new Position(0, 2), new Position(0, 3)), 100, 2)
        ), 300);
        assertEquals(1, timeline.positionAt(150).firstWaypointCount());
        assertEquals(1, timeline.positionAt(150).remainingWaypointIndex());
        assertEquals(0, timeline.positionAt(100).firstWaypointCount());
        assertEquals(1, timeline.positionAt(100).remainingWaypointIndex());
        assertEquals(1, timeline.positionAt(100).position().getLng(), 0.000001);
    }

    @Test
    void shouldSkipZeroDurationAndRepeatedCoordinates() {
        RouteTimeline timeline = new RouteTimeline(List.of(
            new RouteTimeline.Portion(List.of(new Position(0, 0)), 0, 0),
            new RouteTimeline.Portion(List.of(new Position(0, 0), new Position(0, 0), new Position(0, 2)), 100, 0)
        ), 100);
        assertEquals(1, timeline.positionAt(50).position().getLng(), 0.000001);
        assertThrows(IllegalArgumentException.class, () -> timeline.positionAt(0));
        assertThrows(IllegalArgumentException.class, () -> timeline.positionAt(100));
    }
}
