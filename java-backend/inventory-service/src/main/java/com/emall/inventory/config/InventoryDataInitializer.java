package com.emall.inventory.config;

import com.emall.inventory.domain.InventoryEntity;
import com.emall.inventory.repository.InventoryRepository;
import java.math.BigDecimal;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InventoryDataInitializer {

    @Bean
    CommandLineRunner initInventory(InventoryRepository repository) {
        return args -> {
            if (repository.count() > 0) {
                return;
            }

            repository.save(new InventoryEntity(1L, "罗技MX Master 3S", 1L, "华东仓", 118, 6, 10, new BigDecimal("129.00")));
            repository.save(new InventoryEntity(2L, "云原生键盘", 1L, "华东仓", 59, 4, 10, new BigDecimal("239.00")));
            repository.save(new InventoryEntity(3L, "分布式耳机", 1L, "华东仓", 16, 2, 10, new BigDecimal("320.00")));
            repository.save(new InventoryEntity(4L, "联想ThinkVision M14t", 3L, "华北仓", 18, 0, 8, new BigDecimal("620.00")));
        };
    }
}
