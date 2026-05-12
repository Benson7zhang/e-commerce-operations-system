package com.emall.inventory.repository;

import com.emall.inventory.domain.InventoryEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface InventoryRepository extends JpaRepository<InventoryEntity, Long> {

    Optional<InventoryEntity> findByProductId(Long productId);

    @Modifying
    @Transactional
    @Query("UPDATE InventoryEntity i SET i.quantity = i.quantity - :qty "
            + "WHERE i.productId = :productId AND (i.quantity - i.lockedQty) >= :qty")
    int decreaseQuantity(@Param("productId") Long productId, @Param("qty") int qty);

    @Modifying
    @Transactional
    void deleteByProductId(Long productId);
}
