package com.emall.warehouse.config;

import com.emall.warehouse.domain.WarehouseEntity;
import com.emall.warehouse.repository.WarehouseRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WarehouseDataInitializer {

    @Bean
    CommandLineRunner initWarehouses(WarehouseRepository repository) {
        return args -> {
            if (repository.count() > 0) {
                return;
            }

            repository.save(new WarehouseEntity(1L, "华东仓", "华东", 1, "上海市嘉定区"));
            repository.save(new WarehouseEntity(2L, "华南仓", "华南", 1, "广东省广州市"));
            repository.save(new WarehouseEntity(3L, "华北仓", "华北", 1, "北京市大兴区"));
        };
    }
}
