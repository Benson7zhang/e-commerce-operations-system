package com.emall.warehouse.service;

import com.emall.warehouse.domain.WarehouseEntity;
import com.emall.warehouse.repository.WarehouseRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class WarehouseService {

    private final WarehouseRepository repository;

    public WarehouseService(WarehouseRepository repository) {
        this.repository = repository;
    }

    public List<WarehouseEntity> list() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(WarehouseEntity::getId))
                .toList();
    }

    public WarehouseEntity require(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("仓库不存在"));
    }
}
