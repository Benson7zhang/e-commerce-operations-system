package com.emall.order.repository;

import com.emall.order.domain.OrderEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    Optional<OrderEntity> findByOrderNoIgnoreCase(String orderNo);

    List<OrderEntity> findByOrderNoContainingIgnoreCaseOrReceiverNameContainingIgnoreCaseOrReceiverPhoneContainingIgnoreCase(
            String orderNo, String receiverName, String receiverPhone);

    List<OrderEntity> findByStatus(String status);

    @Query("SELECT o FROM OrderEntity o WHERE "
            + "(:search IS NULL OR LOWER(o.orderNo) LIKE LOWER(CONCAT('%', :search, '%')) "
            + "OR LOWER(o.receiverName) LIKE LOWER(CONCAT('%', :search, '%')) "
            + "OR LOWER(o.receiverPhone) LIKE LOWER(CONCAT('%', :search, '%'))) AND "
            + "(:status IS NULL OR o.status = :status)")
    Page<OrderEntity> findByFilters(
            @Param("search") String search,
            @Param("status") String status,
            Pageable pageable);

    @Query("SELECT o.status, COUNT(o) FROM OrderEntity o GROUP BY o.status")
    List<Object[]> countByStatus();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM OrderEntity o WHERE o.status <> '已取消'")
    java.math.BigDecimal sumRevenue();
}
