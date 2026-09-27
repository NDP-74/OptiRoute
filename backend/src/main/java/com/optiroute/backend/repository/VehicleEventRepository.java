package com.optiroute.backend.repository;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.optiroute.backend.entity.VehicleEvent;

public interface VehicleEventRepository extends JpaRepository<VehicleEvent, Long> {
    List<VehicleEvent> findByEventDateGreaterThanEqualAndEventDateLessThan(OffsetDateTime startDate, OffsetDateTime endDate);

    List<VehicleEvent> findByTractor_IdOrderByEventDateDescIdDesc(Long tractorId);

    List<VehicleEvent> findBySemiTrailer_IdOrderByEventDateDescIdDesc(Long semiTrailerId);
}