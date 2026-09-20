package com.optiroute.backend.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.optiroute.backend.entity.VehicleEvent;

public interface VehicleEventRepository extends JpaRepository<VehicleEvent, Long> {
    List<VehicleEvent> findByEventDateBetween(LocalDate startDate, LocalDate endDate);
}