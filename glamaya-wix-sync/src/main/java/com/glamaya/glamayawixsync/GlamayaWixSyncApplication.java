package com.glamaya.glamayawixsync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableIntegration
@IntegrationComponentScan
@EnableScheduling
@ComponentScan(basePackages = {
        "com.glamaya.glamayawixsync",
        "com.glamaya.datacontracts.ecommerce"
})
public class GlamayaWixSyncApplication {

    public static void main(String[] args) {
        SpringApplication.run(GlamayaWixSyncApplication.class, args);
    }

}
