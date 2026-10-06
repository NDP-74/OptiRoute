package com.optiroute.backend.dto.response.route;

import com.optiroute.backend.type.GpsModeType;

public class RouteDto {

    private long duration;
    private long baseDuration;
    private long distanceMeters;
    private GpsModeType type;

    private double originLat;
    private double originLng;

    private double destinationLat;
    private double destinationLng;

    private String polyline;
    private RouteCostDetailsDto costs;

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(long distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public String getPolyline() {
        return polyline;
    }

    public void setPolyline(String polyline) {
        this.polyline = polyline;
    }

    public RouteCostDetailsDto getCosts() {
        return costs;
    }

    public void setCosts(RouteCostDetailsDto costs) {
        this.costs = costs;
    }

    public double getOriginLat() {
        return originLat;
    }

    public void setOriginLat(double originLat) {
        this.originLat = originLat;
    }

    public double getOriginLng() {
        return originLng;
    }

    public void setOriginLng(double originLng) {
        this.originLng = originLng;
    }

    public double getDestinationLat() {
        return destinationLat;
    }

    public void setDestinationLat(double destinationLat) {
        this.destinationLat = destinationLat;
    }

    public double getDestinationLng() {
        return destinationLng;
    }

    public void setDestinationLng(double destinationLng) {
        this.destinationLng = destinationLng;
    }

    public long getBaseDuration() {
        return baseDuration;
    }

    public void setBaseDuration(long baseDuration) {
        this.baseDuration = baseDuration;
    }

    public GpsModeType getType() {
        return type;
    }

    public void setType(GpsModeType type) {
        this.type = type;
    }
}