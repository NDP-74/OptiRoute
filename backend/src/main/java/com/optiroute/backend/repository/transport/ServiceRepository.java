package com.optiroute.backend.repository.transport;

import org.springframework.data.jpa.repository.JpaRepository;

import com.optiroute.backend.entity.transport.Service;
import java.util.List;

public interface ServiceRepository extends JpaRepository<Service, Long> {
    List<Service> findAllByExternalSourceAndExternalId(String externalSource, String externalId);
}
