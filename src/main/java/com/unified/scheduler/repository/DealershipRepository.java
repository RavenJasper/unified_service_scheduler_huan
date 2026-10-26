package com.unified.scheduler.repository;

import com.unified.scheduler.domain.Dealership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DealershipRepository extends JpaRepository<Dealership, Long>, JpaSpecificationExecutor<Dealership> {

    Optional<Dealership> findByCode(String code);

    List<Dealership> findByIsActiveTrueOrderByCodeAscNameAsc();
}
