package com.optiroute.backend.utils;

import com.optiroute.backend.model.Position;

public final class RouteGeometry {

    private static final double EARTH_RADIUS_METERS = 6_371_000;

    private RouteGeometry() {
    }

    public record Projection(double distanceMeters, double fraction) {
    }

    public static double distance(Position from, Position to) {
        return angle(vector(from), vector(to)) * EARTH_RADIUS_METERS;
    }

    public static Projection project(Position point, Position from, Position to) {
        double[] p = vector(point);
        double[] a = vector(from);
        double[] b = vector(to);
        double arc = angle(a, b);
        double distanceToStart = angle(p, a);
        double distanceToEnd = angle(p, b);
        Projection nearest = distanceToStart <= distanceToEnd
            ? new Projection(distanceToStart * EARTH_RADIUS_METERS, 0)
            : new Projection(distanceToEnd * EARTH_RADIUS_METERS, 1);
        double[] normal = cross(a, b);
        double norm = Math.sqrt(dot(normal, normal));
        if (norm < 1e-12) {
            return nearest;
        }
        for (int i = 0; i < 3; i++) normal[i] /= norm;
        double height = dot(p, normal);
        double[] projected = new double[3];
        for (int i = 0; i < 3; i++) projected[i] = p[i] - height * normal[i];
        double projectedNorm = Math.sqrt(dot(projected, projected));
        if (projectedNorm < 1e-12) {
            return nearest;
        }
        for (int i = 0; i < 3; i++) projected[i] /= projectedNorm;
        double fromStart = angle(a, projected);
        double toEnd = angle(projected, b);
        if (Math.abs(fromStart + toEnd - arc) < 1e-10) {
            double distance = angle(p, projected) * EARTH_RADIUS_METERS;
            if (distance < nearest.distanceMeters()) {
                return new Projection(distance, Math.clamp(fromStart / arc, 0, 1));
            }
        }
        return nearest;
    }

    private static double[] vector(Position point) {
        double lat = Math.toRadians(point.getLat());
        double lng = Math.toRadians(point.getLng());
        return new double[] {Math.cos(lat) * Math.cos(lng), Math.cos(lat) * Math.sin(lng), Math.sin(lat)};
    }

    private static double angle(double[] a, double[] b) {
        double[] cross = cross(a, b);
        return Math.atan2(Math.sqrt(dot(cross, cross)), dot(a, b));
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1],
            a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }
}
