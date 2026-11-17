package com.optiroute.backend.repository.transport;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import com.optiroute.backend.entity.transport.Route;

public interface RouteRepository extends JpaRepository<Route, Long> {
    @EntityGraph(attributePaths = "service")
    Optional<Route> findFirstByServiceId(Long serviceId);

    @EntityGraph(attributePaths = "service")
    List<Route> findByServiceIdOrderByIdAsc(Long serviceId);

    @EntityGraph(attributePaths = "service")
    List<Route> findByServiceIdIn(List<Long> serviceIds);

    @EntityGraph(attributePaths = "service")
    List<Route> findByDepartureStartGreaterThanEqualAndDepartureStartLessThan(OffsetDateTime start, OffsetDateTime end);

    @EntityGraph(attributePaths = "service")
    List<Route> findByDriverIdAndDepartureStartGreaterThanEqualAndDepartureStartLessThan(Long driverId, OffsetDateTime start, OffsetDateTime end);

    @EntityGraph(attributePaths = "service")
    List<Route> findByDriverIdInAndDepartureStartGreaterThanEqualAndDepartureStartLessThan(
        List<Long> driverIds,
        OffsetDateTime start,
        OffsetDateTime end
    );

    void deleteByServiceId(Long serviceId);
}
