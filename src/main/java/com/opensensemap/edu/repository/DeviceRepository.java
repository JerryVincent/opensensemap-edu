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
 * Repository for Device entity
 */
@Repository
public interface DeviceRepository extends JpaRepository<Device, String> {

    // Find all public and active devices
    List<Device> findByIsPublicTrueAndStatus(Device.Status status);

    // Find devices by exposure type
    List<Device> findByExposureAndIsPublicTrue(Device.Exposure exposure);

    // Search devices by name
    List<Device> findByNameContainingIgnoreCaseAndIsPublicTrue(String name);

    // Find devices within a bounding box
    @Query("SELECT d FROM Device d WHERE d.isPublic = true " +
            "AND d.latitude BETWEEN :minLat AND :maxLat " +
            "AND d.longitude BETWEEN :minLon AND :maxLon")
    List<Device> findDevicesInBoundingBox(
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLon") Double minLon,
            @Param("maxLon") Double maxLon
    );

    // Count active public devices
    long countByIsPublicTrueAndStatus(Device.Status status);
}