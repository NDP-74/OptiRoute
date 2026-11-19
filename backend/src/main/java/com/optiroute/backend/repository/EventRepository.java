package com.optiroute.backend.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import com.optiroute.backend.entity.event.Event;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByRouteIdOrderByIdAsc(Long routeId);

    @EntityGraph(attributePaths = "costParameter")
    List<Event> findByRouteIdInOrderByRouteIdAscIdAsc(List<Long> routeIds);

    boolean existsByRouteIdIn(List<Long> routeIds);

    long countByTractorId(Long tractorId);

    long countBySemiTrailerId(Long semiTrailerId);

    long countByCostParameterId(Long costParameterId);

    long countByPointOfInterestId(Long pointOfInterestId);

    @Modifying
    @Query("delete from Event e where e.route.id in (select r.id from Route r where r.service.id = :serviceId)")
    int deleteByServiceId(@Param("serviceId") Long serviceId);

    List<Event> findByTractorIdOrderByEventDateDescIdDesc(Long tractorId);

    List<Event> findBySemiTrailerIdOrderByEventDateDescIdDesc(Long semiTrailerId);
}