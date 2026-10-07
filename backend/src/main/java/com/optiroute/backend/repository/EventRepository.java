package com.optiroute.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.optiroute.backend.entity.event.Event;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByRouteIdOrderByIdAsc(Long routeId);
}