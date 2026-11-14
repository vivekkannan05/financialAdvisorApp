package com.financialadvisor;

import com.financialadvisor.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class FinancialAdvisorApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinancialAdvisorApplication.class, args);
    }
}
