package com.emall.returnservice.repository;

import com.emall.returnservice.domain.ReturnEntity;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReturnRepository extends JpaRepository<ReturnEntity, Long> {

    List<ReturnEntity> findByStatus(String status);

    @Query("SELECT r FROM ReturnEntity r WHERE (:status IS NULL OR r.status = :status)")
    Page<ReturnEntity> findByFilters(@Param("status") String status, Pageable pageable);

    @Query("SELECT r.status, COUNT(r) FROM ReturnEntity r GROUP BY r.status")
    List<Object[]> countByStatus();

    @Query("SELECT COALESCE(SUM(r.refundAmount), 0) FROM ReturnEntity r WHERE r.refundAmount IS NOT NULL")
    java.math.BigDecimal sumRefunded();
}
