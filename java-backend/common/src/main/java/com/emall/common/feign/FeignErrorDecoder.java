package com.emall.common.feign;

import feign.Response;
import feign.codec.ErrorDecoder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeignErrorDecoder implements ErrorDecoder {

    private static final Logger log = LoggerFactory.getLogger(FeignErrorDecoder.class);
    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        String body = "";
        try {
            if (response.body() != null) {
                body = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.warn("Failed to read Feign error response body", e);
        }

        log.error("Feign call failed: method={}, status={}, body={}", methodKey, response.status(), body);

        return switch (response.status()) {
            case 404 -> new FeignClientException("Resource not found: " + methodKey, 404);
            case 400 -> new FeignClientException("Bad request: " + body, 400);
            case 503 -> new FeignClientException("Service unavailable: " + methodKey, 503);
            default -> defaultDecoder.decode(methodKey, response);
        };
    }

    public static class FeignClientException extends RuntimeException {
        private final int status;

        public FeignClientException(String message, int status) {
            super(message);
            this.status = status;
        }

        public int getStatus() {
            return status;
        }
    }
}
