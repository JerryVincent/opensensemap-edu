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
 * Repository for Sensor entity
 */
@Repository
public interface SensorRepository extends JpaRepository<Sensor, String> {

    // Find sensors by device
    List<Sensor> findByDeviceId(String deviceId);

    // Find sensors by phenomenon
    List<Sensor> findBySensorWikiPhenomenonContainingIgnoreCase(String phenomenon);

    // Find active sensors with specific phenomenon
    @Query("SELECT s FROM Sensor s JOIN s.device d " +
            "WHERE s.sensorWikiPhenomenon LIKE %:phenomenon% " +
            "AND s.status = 'ACTIVE' " +
            "AND d.isPublic = true")
    List<Sensor> findActiveSensorsByPhenomenon(@Param("phenomenon") String phenomenon);

    // Count sensors by phenomenon
    @Query("SELECT COUNT(s) FROM Sensor s JOIN s.device d " +
            "WHERE s.sensorWikiPhenomenon = :phenomenon " +
            "AND d.isPublic = true " +
            "AND s.status = 'ACTIVE'")
    long countSensorsByPhenomenon(@Param("phenomenon") String phenomenon);
}