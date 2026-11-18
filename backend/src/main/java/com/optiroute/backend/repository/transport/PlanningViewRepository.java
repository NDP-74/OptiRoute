package com.optiroute.backend.repository.transport;

import com.optiroute.backend.entity.transport.PlanningView;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanningViewRepository extends JpaRepository<PlanningView, Long> {
    @EntityGraph(attributePaths = "rows")
    List<PlanningView> findAllByOwnerEmailOrderByNameAscIdAsc(String email);

    Optional<PlanningView> findByIdAndOwnerEmail(Long id, String email);
}
