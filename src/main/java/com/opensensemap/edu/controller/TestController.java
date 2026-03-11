package com.opensensemap.edu.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Test Controller
 * Used to verify database connection and application setup
 */
@RestController
public class TestController {

    @Autowired
    private DataSource dataSource;

    /**
     * Test endpoint - verify application is running
     */
    @GetMapping("/api/status")
    public Map<String, Object> status() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "Application is running!");
        response.put("message", "OpenSenseMap Educational Tool");
        response.put("timestamp", LocalDateTime.now().toString());
        return response;
    }

    /**
     * Test database connection
     */
    @GetMapping("/test")
    public Map<String, Object> testDatabase() {
        Map<String, Object> response = new HashMap<>();

        try {
            // Get connection from pool
            Connection conn = dataSource.getConnection();

            // Test query
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT version(), current_database(), current_timestamp");

            if (rs.next()) {
                response.put("status", "success");
                response.put("database", rs.getString(2));
                response.put("timestamp", rs.getString(3));
                response.put("postgresql_version", rs.getString(1));
            }

            // Check PostGIS
            ResultSet rsPostGIS = stmt.executeQuery("SELECT PostGIS_Version()");
            if (rsPostGIS.next()) {
                response.put("postgis_version", rsPostGIS.getString(1));
            }

            // Close resources
            rsPostGIS.close();
            rs.close();
            stmt.close();
            conn.close();

            response.put("message", "✓ Database connection successful!");

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Database connection failed");
            response.put("error", e.getMessage());
            e.printStackTrace();
        }

        return response;
    }

    /**
     * System information
     */
    @GetMapping("/info")
    public Map<String, Object> systemInfo() {
        Map<String, Object> response = new HashMap<>();

        response.put("java_version", System.getProperty("java.version"));
        response.put("os", System.getProperty("os.name"));
        response.put("user_dir", System.getProperty("user.dir"));
        response.put("spring_boot", "3.2.0");

        return response;
    }
}

// Add at the end of TestController.java (before closing brace)
// Note: You'll need to inject repositories first
/*
@Autowired
private DeviceRepository deviceRepository;

@Autowired
private EduUserRepository eduUserRepository;

@Autowired
private ScenarioRepository scenarioRepository;

@GetMapping("/test/entities")
public Map<String, Object> testEntities() {
    Map<String, Object> response = new HashMap<>();

    try {
        // Count devices
        long deviceCount = deviceRepository.count();
        response.put("device_count", deviceCount);

        // Count users
        long userCount = eduUserRepository.count();
        response.put("edu_user_count", userCount);

        // Count scenarios
        long scenarioCount = scenarioRepository.count();
        response.put("scenario_count", scenarioCount);

        response.put("status", "success");
        response.put("message", "Entity repositories working!");

    } catch (Exception e) {
        response.put("status", "error");
        response.put("error", e.getMessage());
    }

    return response;
}
*/