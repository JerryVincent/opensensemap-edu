package com.opensensemap.edu.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Sensor Entity - Represents an individual sensor on a device
 * Maps to the 'sensor' table in the database
 *
 * SYNC NOTE: Column names must match the OpenSenseMap production schema
 * (Drizzle ORM). Key difference: lastMeasurement is camelCase in production,
 * not snake_case.
 */
@Entity
@Table(name = "sensor")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Sensor {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "title")
    private String title;

    @Column(name = "unit")
    private String unit;

    @Column(name = "sensor_type")
    private String sensorType;

    @Column(name = "icon")
    private String icon;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Device.Status status = Device.Status.INACTIVE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "device_id", nullable = false, insertable = false, updatable = false)
    private String deviceId;

    @Column(name = "sensor_wiki_type")
    private String sensorWikiType;

    @Column(name = "sensor_wiki_phenomenon")
    private String sensorWikiPhenomenon;

    @Column(name = "sensor_wiki_unit")
    private String sensorWikiUnit;

    /**
     * IMPORTANT: The column name in PostgreSQL is "lastMeasurement" (camelCase,
     * quoted identifier) to match the OpenSenseMap production schema.
     *
     * Hibernate by default would look for "last_measurement" due to its
     * ImplicitNamingStrategy. We use the quoted form to force the exact name.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "\"lastMeasurement\"", columnDefinition = "jsonb")
    private Map<String, Object> lastMeasurement;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data", columnDefinition = "jsonb")
    private Map<String, Object> data;

    // Relationship: Many sensors belong to one device
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Helper method to get phenomenon in a user-friendly format
     */
    public String getDisplayPhenomenon() {
        if (sensorWikiPhenomenon != null) {
            return sensorWikiPhenomenon;
        }
        return title != null ? title : "Unknown";
    }

    /**
     * Check if sensor has recent data
     */
    public boolean hasRecentMeasurement() {
        return lastMeasurement != null && !lastMeasurement.isEmpty();
    }
}