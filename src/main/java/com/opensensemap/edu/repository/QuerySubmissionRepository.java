
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
 * Repository for QuerySubmission entity
 */
@Repository
public interface QuerySubmissionRepository extends JpaRepository<QuerySubmission, Long> {

    // Find submissions by user
    List<QuerySubmission> findByUserIdOrderBySubmittedAtDesc(Long userId);

    // Find submissions by task
    List<QuerySubmission> findByTaskIdOrderBySubmittedAtDesc(Long taskId);

    // Find submissions by user and task
    List<QuerySubmission> findByUserIdAndTaskIdOrderByAttemptNumberDesc(Long userId, Long taskId);

    // Count submissions for a task
    long countByTaskId(Long taskId);

    // Count submissions by user
    long countByUserId(Long userId);

    // Count correct submissions for a user
    @Query("SELECT COUNT(DISTINCT qs.taskId) FROM QuerySubmission qs " +
            "WHERE qs.userId = :userId AND qs.isCorrect = true")
    long countCorrectSubmissionsByUser(@Param("userId") Long userId);

    // Get latest submission for user and task
    @Query("SELECT qs FROM QuerySubmission qs " +
            "WHERE qs.userId = :userId AND qs.taskId = :taskId " +
            "ORDER BY qs.submittedAt DESC LIMIT 1")
    Optional<QuerySubmission> findLatestSubmission(
            @Param("userId") Long userId,
            @Param("taskId") Long taskId
    );

    @Query("SELECT COUNT(qs) FROM QuerySubmission qs " +
            "WHERE qs.userId = :userId AND qs.taskId = :taskId AND qs.isCorrect = true")
    long countCorrectByUserAndTask(@Param("userId") Long userId, @Param("taskId") Long taskId);

    // Delete by user ID
    void deleteByUserId(Long userId);
}