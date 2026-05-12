package com.emall.stats.web;

import com.emall.stats.service.StatsAggregationService;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsAggregationService statsAggregationService;

    public StatsController(StatsAggregationService statsAggregationService) {
        this.statsAggregationService = statsAggregationService;
    }

    @GetMapping
    public Map<String, Object> stats() {
        return statsAggregationService.stats();
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        return statsAggregationService.dashboard();
    }

    @GetMapping("/orders/{status}")
    public Map<String, Object> ordersByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int limit
    ) {
        return statsAggregationService.ordersByStatus(status, page, limit);
    }
}
