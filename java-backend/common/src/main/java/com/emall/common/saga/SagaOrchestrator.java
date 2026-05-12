package com.emall.common.saga;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SagaOrchestrator {

    private final String sagaName;
    private final List<SagaStep> steps = new ArrayList<>();

    public SagaOrchestrator(String sagaName) {
        this.sagaName = sagaName;
    }

    public SagaOrchestrator addStep(SagaStep step) {
        steps.add(step);
        return this;
    }

    public SagaContext execute(SagaContext context) {
        List<SagaStep> completed = new ArrayList<>();
        try {
            for (SagaStep step : steps) {
                context = step.action().apply(context);
                completed.add(step);
            }
            return context;
        } catch (Exception ex) {
            Collections.reverse(completed);
            for (SagaStep step : completed) {
                if (step.compensation() != null) {
                    try {
                        step.compensation().accept(context);
                    } catch (Exception compensationEx) {
                        log.warn("Saga compensation failed: {}", step.name(), compensationEx);
                    }
                }
            }
            throw new IllegalStateException("Saga failed: " + sagaName, ex);
        }
    }
}
