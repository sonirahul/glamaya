package com.glamaya.glamayawoocommercesync.config;

import com.glamaya.glamayawoocommercesync.domain.ProcessorStatusService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration class for providing domain services as beans.
 * This ensures that domain services, which encapsulate core business logic,
 * are properly instantiated and available for injection into application services.
 */
@Configuration
public class DomainServiceConfig {

    /**
     * Provides a singleton instance of {@link ProcessorStatusService}.
     *
     * @return A new instance of {@link ProcessorStatusService}.
     */
    @Bean
    public ProcessorStatusService processorStatusService() {
        return new ProcessorStatusService();
    }
}
