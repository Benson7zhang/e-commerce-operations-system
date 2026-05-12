package com.emall.channel;

import com.emall.channel.adapter.ChannelAdapterProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(scanBasePackages = "com.emall")
@EnableConfigurationProperties(ChannelAdapterProperties.class)
public class ChannelServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChannelServiceApplication.class, args);
    }

    @Bean("serviceName")
    String serviceName() {
        return "channel-service";
    }
}
