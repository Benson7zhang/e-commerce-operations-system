package com.emall.product.repository;

import com.emall.product.domain.ProductEntity;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    List<ProductEntity> findByNameContainingIgnoreCaseOrSkuContainingIgnoreCase(String name, String sku);

    List<ProductEntity> findByStatus(String status);

    List<ProductEntity> findByType(String type);

    @Query("SELECT p FROM ProductEntity p WHERE "
            + "(:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) "
            + "OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%'))) AND "
            + "(:typeFilter IS NULL OR p.type = :typeFilter) AND "
            + "(:status IS NULL OR p.status = :status)")
    Page<ProductEntity> findByFilters(
            @Param("search") String search,
            @Param("typeFilter") String typeFilter,
            @Param("status") String status,
            Pageable pageable);

    @Query("SELECT p.status, COUNT(p) FROM ProductEntity p GROUP BY p.status")
    List<Object[]> countByStatus();
}
