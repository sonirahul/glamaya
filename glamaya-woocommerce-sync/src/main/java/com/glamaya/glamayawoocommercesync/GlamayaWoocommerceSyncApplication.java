package com.glamaya.glamayawoocommercesync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.glamaya.glamayawoocommercesync",
        "com.glamaya.datacontracts.ecommerce"
})
public class GlamayaWoocommerceSyncApplication {
    public static void main(String[] args) {
        SpringApplication.run(GlamayaWoocommerceSyncApplication.class, args);
    }
}
