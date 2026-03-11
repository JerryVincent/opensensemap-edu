package com.opensensemap.edu.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Device Entity - Represents an OpenSenseMap sensor box/station
 * Maps to the 'device' table in the database
 *
 * SYNC NOTE: Includes 'website' and 'apiKey' columns to match the
 * OpenSenseMap production schema. apiKey uses a quoted identifier
 * because the production column is camelCase.
 */
@Entity
@Table(name = "device")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Device {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "image")
    private String image;

    @Column(name = "website")
    private String website;

    @Column(name = "description")
    private String description;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tags", columnDefinition = "text[]")
    private String[] tags;

    @Column(name = "link")
    private String link;

    @Column(name = "use_auth")
    private Boolean useAuth;

    /**
     * IMPORTANT: The column name in PostgreSQL is "apiKey" (camelCase,
     * quoted identifier) to match the OpenSenseMap production schema.
     */
    @Column(name = "\"apiKey\"")
    private String apiKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "exposure")
    private Exposure exposure;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status = Status.INACTIVE;

    @Column(name = "model")
    private Model model;

    @Column(name = "public")
    private Boolean isPublic = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "expires_at")
    private LocalDate expiresAt;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "sensor_wiki_model")
    private String sensorWikiModel;

    // Relationship: One device has many sensors
    @OneToMany(mappedBy = "device", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Sensor> sensors = new ArrayList<>();

    // Enums - values are UPPERCASE in Java but stored as lowercase in PostgreSQL
    // The @Enumerated(EnumType.STRING) with PostgreSQL enum types handles the mapping
    public enum Exposure {
        INDOOR, OUTDOOR, MOBILE, UNKNOWN
    }

    public enum Status {
        ACTIVE, INACTIVE, OLD
    }

    public enum Model {
        HOMEV2LORA("homeV2Lora"),
        HOMEV2ETHERNET("homeV2Ethernet"),
        HOMEV2WIFI("homeV2Wifi"),
        SENSEBOX_EDU("senseBox:Edu"),
        LUFTDATEN_INFO("luftdaten.info"),
        CUSTOM("custom");

        private final String value;

        Model(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}