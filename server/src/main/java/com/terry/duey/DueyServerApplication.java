package com.terry.duey;

import com.terry.duey.config.DueyProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(DueyProperties.class)
public class DueyServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(DueyServerApplication.class, args);
    }
}
