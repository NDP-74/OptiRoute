package com.optiroute.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.optiroute.backend.dto.request.PointOfInterestRequest;
import com.optiroute.backend.dto.response.PointOfInterestResponse;
import com.optiroute.backend.service.PointOfInterestService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/points-of-interest")
public class PointOfInterestController {

    private final PointOfInterestService pointOfInterestService;

    public PointOfInterestController(PointOfInterestService pointOfInterestService) {
        this.pointOfInterestService = pointOfInterestService;
    }

    @PostMapping
    public ResponseEntity<PointOfInterestResponse> create(@Valid @RequestBody PointOfInterestRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pointOfInterestService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<PointOfInterestResponse>> getAll() {
        return ResponseEntity.ok(pointOfInterestService.getAll());
    }
}
