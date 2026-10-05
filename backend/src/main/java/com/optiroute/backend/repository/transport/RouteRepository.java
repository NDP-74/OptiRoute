package com.optiroute.backend.repository.transport;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.optiroute.backend.entity.transport.Route;

public interface RouteRepository extends JpaRepository<Route, Long> {
    Optional<Route> findFirstByServiceId(Long serviceId);

    List<Route> findByStartDateGreaterThanEqualAndStartDateLessThan(OffsetDateTime start, OffsetDateTime end);

    void deleteByServiceId(Long serviceId);
}
