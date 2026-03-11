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
 * Repository for Scenario entity
 */
@Repository
public interface ScenarioRepository extends JpaRepository<Scenario, Long> {

    // Find published scenarios
    List<Scenario> findByIsPublishedTrueOrderByOrderIndexAsc();

    // Find scenarios by difficulty
    List<Scenario> findByDifficultyLevelAndIsPublishedTrue(Integer difficultyLevel);

    // Find scenarios with tasks
    @Query("SELECT DISTINCT s FROM Scenario s LEFT JOIN FETCH s.tasks " +
            "WHERE s.isPublished = true ORDER BY s.orderIndex")
    List<Scenario> findPublishedScenariosWithTasks();
}