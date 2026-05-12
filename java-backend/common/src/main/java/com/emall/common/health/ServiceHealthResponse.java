package com.emall.common.health;

import java.util.Map;

public record ServiceHealthResponse(
        String service,
        String status,
        String database,
        Map<String, Object> infrastructure
) {
}
