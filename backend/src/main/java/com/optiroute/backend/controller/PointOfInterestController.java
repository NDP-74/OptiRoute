package com.optiroute.backend.controller;

import java.util.List;
import java.time.Instant;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.optiroute.backend.dto.request.PointOfInterestRequest;
import com.optiroute.backend.dto.response.PointOfInterestResponse;
import com.optiroute.backend.dto.response.ApiErrorResponse;
import com.optiroute.backend.service.PointOfInterestService;

import jakarta.validation.Valid;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;

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

    @PutMapping("/{id}")
    public ResponseEntity<PointOfInterestResponse> update(@PathVariable Long id, @Valid @RequestBody PointOfInterestRequest request) {
        return ResponseEntity.ok(pointOfInterestService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        pointOfInterestService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(EntityNotFoundException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiErrorResponse(
                HttpStatus.NOT_FOUND.value(), "NOT_FOUND", "Point d'intérêt introuvable.",
                request.getRequestURI(), Instant.now()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(DataIntegrityViolationException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiErrorResponse(
                HttpStatus.CONFLICT.value(), "POINT_OF_INTEREST_IN_USE",
                "Ce point d'intérêt est utilisé par un événement et ne peut pas être supprimé.",
                request.getRequestURI(), Instant.now()));
    }
}
