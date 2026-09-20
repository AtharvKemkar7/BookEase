package com.bookease.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(
        value = "bookease.reminders.processing-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class SchedulingConfiguration {
}
