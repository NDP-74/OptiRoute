package com.optiroute.backend.dto.request.route;

import com.optiroute.backend.model.Position;
import com.optiroute.backend.type.GpsModeType;
import com.optiroute.backend.type.RouteTimeMode;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class RouteRequest {

    private Position origin;
    private Position destination;
    private List<Position> waypoints = new ArrayList<>();

    private GpsModeType mode;

    private OffsetDateTime routeTime;
    private RouteTimeMode timeMode = RouteTimeMode.DEPARTURE;

    private Long tractorId;
    private Long semiTrailerId;
    private Long driverId;

    private boolean emptyTrip;

    public Position getOrigin() {
        return origin;
    }

    public void setOrigin(Position origin) {
        this.origin = origin;
    }

    public Position getDestination() {
        return destination;
    }

    public void setDestination(Position destination) {
        this.destination = destination;
    }

    public List<Position> getWaypoints() {
        return waypoints;
    }

    public void setWaypoints(List<Position> waypoints) {
        this.waypoints = waypoints == null ? new ArrayList<>() : waypoints;
    }

    public GpsModeType getMode() {
        return mode;
    }

    public void setMode(GpsModeType mode) {
        this.mode = mode;
    }

    public OffsetDateTime getRouteTime() {
        return routeTime;
    }

    public void setRouteTime(OffsetDateTime routeTime) {
        this.routeTime = routeTime;
    }

    public RouteTimeMode getTimeMode() {
        return timeMode;
    }

    public void setTimeMode(RouteTimeMode timeMode) {
        this.timeMode = timeMode == null ? RouteTimeMode.DEPARTURE : timeMode;
    }

    public Long getTractorId() {
        return tractorId;
    }

    public void setTractorId(Long tractorId) {
        this.tractorId = tractorId;
    }

    public Long getSemiTrailerId() {
        return semiTrailerId;
    }

    public void setSemiTrailerId(Long semiTrailerId) {
        this.semiTrailerId = semiTrailerId;
    }

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }

    public boolean isEmptyTrip() {
        return emptyTrip;
    }

    public void setEmptyTrip(boolean emptyTrip) {
        this.emptyTrip = emptyTrip;
    }

}