package com.emall.order.config;

import com.emall.order.domain.OrderEntity;
import com.emall.order.repository.OrderRepository;
import java.math.BigDecimal;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderDataInitializer {

    @Bean
    CommandLineRunner initOrders(OrderRepository repository) {
        return args -> {
            if (repository.count() > 0) {
                return;
            }

            repository.save(new OrderEntity("JAVA-ORDER-001", new BigDecimal("398.00"), "配送中",
                    "张三", "13800000001", "上海市浦东新区陆家嘴环路1000号",
                    "SF00100001", "顺丰", "上海分拨中心", "罗技MX Master 3S(2)", 1L, 2, "2026-05-07 09:40"));
            repository.save(new OrderEntity("JAVA-ORDER-002", new BigDecimal("399.00"), "配送中",
                    "李四", "13800000002", "杭州市滨江区网商路699号",
                    "YT00100002", "圆通", "杭州派件中心", "云原生键盘(1)", 2L, 1, "2026-05-06 12:40"));
            repository.save(new OrderEntity("JAVA-ORDER-003", new BigDecimal("599.00"), "已签收",
                    "王五", "13800000003", "北京市朝阳区建国路88号",
                    "JD00100003", "京东", "已签收-本人", "分布式耳机(1)", 3L, 1, "2026-05-05 12:40"));
            repository.save(new OrderEntity("JAVA-ORDER-004", new BigDecimal("1798.00"), "配送中",
                    "赵六", "13800000004", "深圳市南山区科技园中一路8号",
                    "ZT00100004", "中通", "苏州中转中心", "联想ThinkVision M14t(2)", 4L, 2, "2026-05-07 14:00"));
        };
    }
}
