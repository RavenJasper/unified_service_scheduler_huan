package com.unified.scheduler.repository;

import com.unified.scheduler.domain.ServiceBay;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceBayRepository extends JpaRepository<ServiceBay, Long>, JpaSpecificationExecutor<ServiceBay> {

    List<ServiceBay> findByDealershipIdAndIsOperationalTrueOrderByBayNumberAscNameAsc(Long dealershipId);

    List<ServiceBay> findByIsOperationalTrueOrderByDealershipIdAscBayNumberAscNameAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM ServiceBay b WHERE b.id = :id")
    Optional<ServiceBay> findByIdWithLock(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM ServiceBay b WHERE b.id IN :ids ORDER BY b.id ASC")
    List<ServiceBay> findAllByIdWithLock(@Param("ids") List<Long> ids);
}
