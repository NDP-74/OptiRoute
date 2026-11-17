package com.optiroute.backend.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

public class Position {

    @DecimalMin("-90")
    @DecimalMax("90")
    private double lat;
    @DecimalMin("-180")
    @DecimalMax("180")
    private double lng;
    private String name;
    private String address;
    // Renseigné uniquement pour les étapes issues d'un événement lors d'une replanification, jamais stocké sur le trajet
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long eventId;

    public Position() {
    }

    public Position(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    public double getLat() {
        return lat;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }
}