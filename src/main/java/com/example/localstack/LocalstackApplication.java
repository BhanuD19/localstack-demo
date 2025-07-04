package com.example.localstack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LocalstackApplication {
    public static void main(String[] args) {
        SpringApplication.run(LocalstackApplication.class, args);
    }
}
