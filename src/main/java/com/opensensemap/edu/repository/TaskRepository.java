
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
 * Repository for Task entity
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // Find tasks by scenario
    List<Task> findByScenarioIdOrderByTaskOrderAsc(Long scenarioId);

    // Find task by scenario and order
    Optional<Task> findByScenarioIdAndTaskOrder(Long scenarioId, Integer taskOrder);

    // Count tasks in scenario
    long countByScenarioId(Long scenarioId);
}