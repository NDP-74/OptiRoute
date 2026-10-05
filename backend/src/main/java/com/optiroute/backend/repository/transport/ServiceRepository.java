package com.optiroute.backend.repository.transport;

import org.springframework.data.jpa.repository.JpaRepository;

import com.optiroute.backend.entity.transport.Service;

public interface ServiceRepository extends JpaRepository<Service, Long> {
}
