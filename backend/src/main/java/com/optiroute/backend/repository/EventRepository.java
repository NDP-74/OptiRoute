package com.optiroute.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import com.optiroute.backend.entity.event.Event;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByRouteIdOrderByIdAsc(Long routeId);

    @EntityGraph(attributePaths = "costParameter")
    List<Event> findByRouteIdInOrderByRouteIdAscIdAsc(List<Long> routeIds);

    boolean existsByRouteIdIn(List<Long> routeIds);

    List<Event> findByTractorIdOrderByEventDateDescIdDesc(Long tractorId);

    List<Event> findBySemiTrailerIdOrderByEventDateDescIdDesc(Long semiTrailerId);
}