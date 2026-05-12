package com.emall.common.saga;

import java.util.function.Consumer;
import java.util.function.Function;

public record SagaStep(
        String name,
        Function<SagaContext, SagaContext> action,
        Consumer<SagaContext> compensation
) {
}
