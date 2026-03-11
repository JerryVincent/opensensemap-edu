
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
 * Repository for UserProgress entity
 */
@Repository
public interface UserProgressRepository extends JpaRepository<UserProgress, Long> {

    // Find progress by user
    List<UserProgress> findByUserIdOrderByLastActivityDesc(Long userId);

    // Find progress by user and scenario
    Optional<UserProgress> findByUserIdAndScenarioId(Long userId, Long scenarioId);

    // Find completed scenarios for user
    List<UserProgress> findByUserIdAndStatus(Long userId, UserProgress.ProgressStatus status);

    // Find in-progress scenarios for user
    @Query("SELECT up FROM UserProgress up " +
            "WHERE up.userId = :userId " +
            "AND up.status = 'IN_PROGRESS' " +
            "ORDER BY up.lastActivity DESC")
    List<UserProgress> findInProgressScenarios(@Param("userId") Long userId);

    // Calculate total score for user
    @Query("SELECT COALESCE(SUM(up.score), 0) FROM UserProgress up " +
            "WHERE up.userId = :userId")
    Integer calculateTotalScore(@Param("userId") Long userId);

    // Delete by user ID
    void deleteByUserId(Long userId);
}