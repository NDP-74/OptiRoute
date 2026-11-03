package com.optiroute.backend.model;

import java.util.List;

import com.optiroute.backend.utils.RouteGeometry;

public record RouteTimeline(List<Portion> portions, long durationSeconds) {

    public record Portion(List<Position> geometry, double durationSeconds, int sectionIndex) {
    }

    public record Cut(Position position, int firstWaypointCount, int remainingWaypointIndex) {
    }

    public record Proximity(double distanceMeters, int firstWaypointCount, int remainingWaypointIndex) {
    }

    public Proximity proximityTo(Position position) {
        Proximity nearest = null;
        for (int index = 0; index < portions.size(); index++) {
            Portion portion = portions.get(index);
            List<Position> geometry = portion.geometry();
            for (int i = 0; i < geometry.size(); i++) {
                RouteGeometry.Projection projection = RouteGeometry.project(position,
                    geometry.get(i), geometry.get(Math.min(i + 1, geometry.size() - 1)));
                if (nearest == null || projection.distanceMeters() < nearest.distanceMeters()) {
                    boolean sectionEnd = (index + 1 == portions.size()
                        || portions.get(index + 1).sectionIndex() != portion.sectionIndex())
                        && (i == geometry.size() - 1 || (i == geometry.size() - 2 && projection.fraction() == 1));
                    int passedWaypoints = portion.sectionIndex() + (sectionEnd ? 1 : 0);
                    nearest = new Proximity(projection.distanceMeters(), passedWaypoints, passedWaypoints);
                }
            }
        }
        if (nearest == null) {
            throw new IllegalStateException("Route geometry is missing for the parking proximity check");
        }
        return nearest;
    }

    public Cut positionAt(double elapsedSeconds) {
        if (elapsedSeconds <= 0 || elapsedSeconds >= durationSeconds) {
            throw new IllegalArgumentException("The cut must be inside the driving interval");
        }
        double elapsed = 0;
        double sectionStartTime = 0;
        int previousSection = -1;
        for (Portion portion : portions) {
            if (previousSection != portion.sectionIndex()) {
                sectionStartTime = elapsed;
            }
            if (portion.durationSeconds() > 0 && elapsedSeconds + 1e-7 < elapsed + portion.durationSeconds()) {
                boolean atWaypoint = portion.sectionIndex() > 0 && Math.abs(elapsedSeconds - sectionStartTime) < 1e-7;
                double fraction = Math.clamp((elapsedSeconds - elapsed) / portion.durationSeconds(), 0, 1);
                return new Cut(interpolate(portion.geometry(), fraction),
                    portion.sectionIndex() - (atWaypoint ? 1 : 0), portion.sectionIndex());
            }
            elapsed += portion.durationSeconds();
            previousSection = portion.sectionIndex();
        }
        throw new IllegalStateException("Timed geometry does not cover the requested cut");
    }

    private Position interpolate(List<Position> geometry, double fraction) {
        double total = 0;
        for (int i = 1; i < geometry.size(); i++) {
            total += RouteGeometry.distance(geometry.get(i - 1), geometry.get(i));
        }
        double remaining = total * fraction;
        for (int i = 1; i < geometry.size(); i++) {
            Position from = geometry.get(i - 1);
            Position to = geometry.get(i);
            double length = RouteGeometry.distance(from, to);
            if (length > 0 && remaining <= length) {
                double ratio = remaining / length;
                return new Position(from.getLat() + (to.getLat() - from.getLat()) * ratio,
                    normalizeLongitude(from.getLng() + longitudeDelta(from, to) * ratio));
            }
            remaining -= length;
        }
        return geometry.getLast();
    }

    private static double normalizeLongitude(double longitude) {
        return longitude > 180 ? longitude - 360 : longitude < -180 ? longitude + 360 : longitude;
    }

    private static double longitudeDelta(Position from, Position to) {
        double delta = to.getLng() - from.getLng();
        return delta > 180 ? delta - 360 : delta < -180 ? delta + 360 : delta;
    }
}
