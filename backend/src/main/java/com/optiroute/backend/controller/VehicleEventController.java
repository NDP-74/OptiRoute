package com.optiroute.backend.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.optiroute.backend.dto.request.VehicleEventRequest;
import com.optiroute.backend.dto.response.VehicleEventResponse;
import com.optiroute.backend.service.VehicleEventService;

@RestController
@RequestMapping("/api/vehicle-events")
public class VehicleEventController {

    private final VehicleEventService vehicleEventService;

    public VehicleEventController(VehicleEventService vehicleEventService) {
        this.vehicleEventService = vehicleEventService;
    }

    @PostMapping
    public ResponseEntity<VehicleEventResponse> create(@RequestBody VehicleEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleEventService.create(request));
    }

    @GetMapping("/by-range")
    public ResponseEntity<List<VehicleEventResponse>> getByDateRange(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(vehicleEventService.getByDateRange(startDate,endDate));
    }

    @GetMapping("/by-tractor/{tractorId}")
    public ResponseEntity<List<VehicleEventResponse>> getByTractorId(@PathVariable Long tractorId) {
        return ResponseEntity.ok(vehicleEventService.getByTractorId(tractorId));
    }

    @GetMapping("/by-semi-trailer/{semiTrailerId}")
    public ResponseEntity<List<VehicleEventResponse>> getBySemiTrailerId(@PathVariable Long semiTrailerId) {
        return ResponseEntity.ok(vehicleEventService.getBySemiTrailerId(semiTrailerId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleEventResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(vehicleEventService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VehicleEventResponse> update(@PathVariable Long id, @RequestBody VehicleEventRequest request) {
        return ResponseEntity.ok(vehicleEventService.update(id,request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        vehicleEventService.delete(id);
        return ResponseEntity.noContent().build();
    }
}