package com.glamaya.glamayawoocommercesync.config;

import com.glamaya.glamayawoocommercesync.domain.ProcessorStatusService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfig {

    @Bean
    public ProcessorStatusService processorStatusService() {
        return new ProcessorStatusService();
    }
}
