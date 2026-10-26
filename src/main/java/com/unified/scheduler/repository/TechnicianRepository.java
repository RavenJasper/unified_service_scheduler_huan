package com.unified.scheduler.repository;

import com.unified.scheduler.domain.Technician;
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
public interface TechnicianRepository extends JpaRepository<Technician, Long>, JpaSpecificationExecutor<Technician> {

    @Query("SELECT DISTINCT t FROM Technician t JOIN t.skills s " +
           "WHERE t.dealership.id = :dealershipId " +
           "AND t.isActive = true " +
           "AND s.skillCode = :skillCode " +
           "ORDER BY t.employeeCode ASC, t.lastName ASC, t.firstName ASC")
    List<Technician> findActiveTechniciansWithSkill(@Param("dealershipId") Long dealershipId,
                                                   @Param("skillCode") String skillCode);

    @Query("SELECT DISTINCT t FROM Technician t LEFT JOIN FETCH t.skills " +
           "WHERE t.dealership.id = :dealershipId AND t.isActive = true " +
           "ORDER BY t.dealership.id ASC, t.employeeCode ASC, t.lastName ASC, t.firstName ASC")
    List<Technician> findActiveByDealershipIdWithSkills(@Param("dealershipId") Long dealershipId);

    @Query("SELECT DISTINCT t FROM Technician t LEFT JOIN FETCH t.skills " +
           "WHERE t.isActive = true " +
           "ORDER BY t.dealership.id ASC, t.employeeCode ASC, t.lastName ASC, t.firstName ASC")
    List<Technician> findAllActiveWithSkills();

    @Query("SELECT t FROM Technician t LEFT JOIN FETCH t.skills WHERE t.id = :id")
    Optional<Technician> findByIdWithSkills(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Technician t WHERE t.id = :id")
    Optional<Technician> findByIdWithLock(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Technician t WHERE t.id IN :ids ORDER BY t.id ASC")
    List<Technician> findAllByIdWithLock(@Param("ids") List<Long> ids);
}
