package com.emall.channel.repository;

import com.emall.channel.domain.ChannelOrderEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChannelOrderRepository extends JpaRepository<ChannelOrderEntity, Long> {

    Optional<ChannelOrderEntity> findByChannelCodeAndPlatformOrderId(String channelCode, String platformOrderId);

    List<ChannelOrderEntity> findByChannelCodeOrderByOrderTimeDesc(String channelCode);

    List<ChannelOrderEntity> findByUnifiedStatusOrderByOrderTimeDesc(String unifiedStatus);

    List<ChannelOrderEntity> findAllByOrderTimeDesc();

    @Query("SELECT c FROM ChannelOrderEntity c WHERE "
            + "(:channelCode IS NULL OR c.channelCode = :channelCode) AND "
            + "(:status IS NULL OR c.unifiedStatus = :status)")
    Page<ChannelOrderEntity> findByFilters(
            @Param("channelCode") String channelCode,
            @Param("status") String status,
            Pageable pageable);
}
