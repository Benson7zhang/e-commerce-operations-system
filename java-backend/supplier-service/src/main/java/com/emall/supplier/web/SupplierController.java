package com.emall.supplier.web;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    @GetMapping
    public List<Map<String, Object>> listSuppliers() {
        return List.of(Map.of("id", 1, "name", "Java供应商", "status", 1));
    }
}
