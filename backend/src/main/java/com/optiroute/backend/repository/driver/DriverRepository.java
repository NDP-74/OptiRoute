package com.optiroute.backend.repository.driver;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import com.optiroute.backend.entity.driver.Driver;
import java.util.List;

public interface DriverRepository extends JpaRepository<Driver, Long> {
    boolean existsByLogin(String login);

    @Override
    @EntityGraph(attributePaths = {"tractor", "semiTrailer"})
    List<Driver> findAll();
}