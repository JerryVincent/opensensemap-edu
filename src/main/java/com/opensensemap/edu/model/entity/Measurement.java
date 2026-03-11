package com.opensensemap.edu.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.ZonedDateTime;

/**
 * Measurement Entity - Represents a sensor reading
 * Maps to the 'measurement' table in the database
 *
 * Note: This uses a composite key (sensorId + time) since measurements
 * don't have a single primary key
 */
@Entity
@Table(name = "measurement")
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(Measurement.MeasurementId.class)
public class Measurement {

    @Id
    @Column(name = "sensor_id", nullable = false)
    private String sensorId;

    @Id
    @Column(name = "time", nullable = false, columnDefinition = "TIMESTAMP(3) WITH TIME ZONE")
    private ZonedDateTime time;

    @Column(name = "value")
    private Double value;

    @Column(name = "location_id")
    private Long locationId;

    /**
     * Composite Primary Key class for Measurement
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MeasurementId implements Serializable {
        private String sensorId;
        private ZonedDateTime time;
    }

    /**
     * Check if measurement has a valid value
     */
    public boolean hasValidValue() {
        return value != null && !value.isNaN() && !value.isInfinite();
    }

    /**
     * Get value rounded to specified decimal places
     */
    public Double getValueRounded(int decimalPlaces) {
        if (value == null) return null;
        double multiplier = Math.pow(10, decimalPlaces);
        return Math.round(value * multiplier) / multiplier;
    }
}