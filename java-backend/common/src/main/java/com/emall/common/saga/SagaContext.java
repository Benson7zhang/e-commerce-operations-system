package com.emall.common.saga;

import java.util.HashMap;
import java.util.Map;

public class SagaContext {

    private final Map<String, Object> values = new HashMap<>();

    public SagaContext put(String key, Object value) {
        values.put(key, value);
        return this;
    }

    public Object get(String key) {
        return values.get(key);
    }

    public Map<String, Object> snapshot() {
        return Map.copyOf(values);
    }
}
