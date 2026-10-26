package com.unified.scheduler.repository;

import com.unified.scheduler.domain.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceTypeRepository extends JpaRepository<ServiceType, Long>, JpaSpecificationExecutor<ServiceType> {

    Optional<ServiceType> findByCode(String code);

    List<ServiceType> findByIsActiveTrueOrderByCodeAscNameAsc();
}
