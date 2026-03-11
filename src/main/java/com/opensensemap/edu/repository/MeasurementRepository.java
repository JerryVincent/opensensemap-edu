package com.opensensemap.edu.repository;

import com.opensensemap.edu.model.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Measurement entity
 */
@Repository
public interface MeasurementRepository extends JpaRepository<Measurement, Measurement.MeasurementId> {

    // Find measurements by sensor ID
    List<Measurement> findBySensorIdOrderByTimeDesc(String sensorId);

    // Find recent measurements for a sensor
    @Query("SELECT m FROM Measurement m " +
            "WHERE m.sensorId = :sensorId " +
            "AND m.time >= :since " +
            "ORDER BY m.time DESC")
    List<Measurement> findRecentMeasurements(
            @Param("sensorId") String sensorId,
            @Param("since") ZonedDateTime since
    );

    // Find measurements in time range
    @Query("SELECT m FROM Measurement m " +
            "WHERE m.sensorId = :sensorId " +
            "AND m.time BETWEEN :start AND :end " +
            "ORDER BY m.time ASC")
    List<Measurement> findMeasurementsInRange(
            @Param("sensorId") String sensorId,
            @Param("start") ZonedDateTime start,
            @Param("end") ZonedDateTime end
    );

    // Calculate average value for a sensor in time range
    @Query("SELECT AVG(m.value) FROM Measurement m " +
            "WHERE m.sensorId = :sensorId " +
            "AND m.time BETWEEN :start AND :end " +
            "AND m.value IS NOT NULL")
    Double calculateAverageValue(
            @Param("sensorId") String sensorId,
            @Param("start") ZonedDateTime start,
            @Param("end") ZonedDateTime end
    );
}