package com.bookease.config;

import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfiguration {

    @Bean
    FlywayConfigurationCustomizer flywayConfigurationCustomizer() {
        return configuration -> configuration
                .baselineOnMigrate(false)
                .validateOnMigrate(true)
                .outOfOrder(false);
    }
}
