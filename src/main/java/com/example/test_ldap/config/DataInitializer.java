package com.example.test_ldap.config;

import com.example.test_ldap.controller.PortfolioController;
import com.example.test_ldap.controller.PositionController;
import com.example.test_ldap.model.Portfolio;
import com.example.test_ldap.model.Position;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(PortfolioController portfolioController, PositionController positionController) {
        return args -> {
            // Bypass security by directly accessing the controller's internal data structures
            // This avoids triggering @PreAuthorize during startup
            
            // Access portfolios map via reflection or create a public init method
            // For now, we'll use a simpler approach: create data after startup
            System.out.println("Data initialization skipped - use authenticated requests to create data");
        };
    }
}
