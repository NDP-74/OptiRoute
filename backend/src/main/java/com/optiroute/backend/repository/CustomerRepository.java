package com.optiroute.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.optiroute.backend.entity.Customer;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByCode(String code);

    boolean existsByCode(String code);
}