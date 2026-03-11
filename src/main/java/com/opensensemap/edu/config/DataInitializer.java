package com.opensensemap.edu.config;

import com.opensensemap.edu.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Data Initializer
 * Creates default users on application startup
 */
@Configuration
public class DataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initializeData(UserService userService) {
        return args -> {
            logger.info("Checking for default users...");
            
            // Create default admin user
            userService.createDefaultAdminIfNotExists();
            logger.info("Admin user ready (username: admin, password: admin123)");
            
            // Create demo student user
            userService.createDemoStudentIfNotExists();
            logger.info("Demo student ready (username: student, password: student123)");
        };
    }
}
