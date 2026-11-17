package com.optiroute.backend.repository.vehicle;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import com.optiroute.backend.entity.vehicle.SemiTrailer;

import java.util.List;
import java.util.Optional;

public interface SemiTrailerRepository extends JpaRepository<SemiTrailer, Long> {
    Optional<SemiTrailer> findByRegistration(String registration);

    boolean existsByRegistration(String registration);

    boolean existsByTrailerTypeId(Long trailerTypeId);

    @EntityGraph(attributePaths = "trailerType")
    List<SemiTrailer> findWithTrailerTypeByIdIn(List<Long> ids);
}