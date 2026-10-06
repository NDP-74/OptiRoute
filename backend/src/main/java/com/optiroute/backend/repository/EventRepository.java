package com.optiroute.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.optiroute.backend.entity.event.Event;

public interface EventRepository extends JpaRepository<Event, Long> {
}