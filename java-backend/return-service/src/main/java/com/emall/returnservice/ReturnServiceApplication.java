package com.emall.returnservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(scanBasePackages = "com.emall")
@EnableFeignClients(basePackages = "com.emall.common.feign")
public class ReturnServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReturnServiceApplication.class, args);
    }

    @Bean("serviceName")
    String serviceName() {
        return "return-service";
    }
}
