package com.emall.product.config;

import com.emall.product.domain.ProductEntity;
import com.emall.product.repository.ProductRepository;
import java.math.BigDecimal;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProductDataInitializer {

    @Bean
    CommandLineRunner initProducts(ProductRepository repository) {
        return args -> {
            if (repository.count() > 0) {
                return;
            }

            repository.save(new ProductEntity("JAVA001", "罗技MX Master 3S", "电脑配件",
                    new BigDecimal("199.00"), new BigDecimal("129.00"), "在售", 118, 6));
            repository.save(new ProductEntity("JAVA002", "云原生键盘", "电脑配件",
                    new BigDecimal("399.00"), new BigDecimal("239.00"), "在售", 59, 4));
            repository.save(new ProductEntity("JAVA003", "分布式耳机", "耳机音箱",
                    new BigDecimal("599.00"), new BigDecimal("320.00"), "下架", 16, 2));
            repository.save(new ProductEntity("JAVA004", "联想ThinkVision M14t", "电脑配件",
                    new BigDecimal("899.00"), new BigDecimal("620.00"), "在售", 18, 0));
        };
    }
}
