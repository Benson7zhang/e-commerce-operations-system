package com.emall.returnservice.config;

import com.emall.returnservice.domain.ReturnEntity;
import com.emall.returnservice.repository.ReturnRepository;
import java.math.BigDecimal;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReturnDataInitializer {

    @Bean
    CommandLineRunner initReturns(ReturnRepository repository) {
        return args -> {
            if (repository.count() > 0) {
                return;
            }

            repository.save(new ReturnEntity(1L, "JAVA-ORDER-001", "张三",
                    new BigDecimal("398.00"), "商品质量问题", "待处理", null, null,
                    "2026-05-07 09:43", 1L, 1));
            repository.save(new ReturnEntity(2L, "JAVA-ORDER-002", "李四",
                    new BigDecimal("399.00"), "商品与描述不符", "已完成", new BigDecimal("399.00"), null,
                    "2026-05-06 12:43", 2L, 1));
        };
    }
}
