package com.glamaya.sync.runner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * The main Spring Boot application class for the Glamaya Sync Service.
 * This class serves as the entry point for the runner module,
 * enabling component scanning, auto-configuration, and scheduling.
 */
@SpringBootApplication(scanBasePackages = {"com.glamaya.sync.core", "com.glamaya.sync.platform.woocommerce", "com.glamaya.sync.runner"})
@EnableScheduling // Enable Spring's scheduled task execution
public class GlamayaSyncApplication {

    public static void main(String[] args) {
        SpringApplication.run(GlamayaSyncApplication.class, args);
    }
}
