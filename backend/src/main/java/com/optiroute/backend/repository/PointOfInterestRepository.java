package com.optiroute.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.optiroute.backend.entity.event.PointOfInterest;

public interface PointOfInterestRepository extends JpaRepository<PointOfInterest, Long> {
}
