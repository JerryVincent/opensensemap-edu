package com.opensensemap.edu.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * QuerySubmission Entity - Represents a student's SQL query attempt
 * Maps to the 'query_submission' table in the database
 */
@Entity
@Table(name = "query_submission")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuerySubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", insertable = false, updatable = false)
    private Long userId;

    @Column(name = "task_id", insertable = false, updatable = false)
    private Long taskId;

    @Column(name = "query_text", nullable = false, columnDefinition = "TEXT")
    private String queryText;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "execution_time_ms")
    private Integer executionTimeMs;

    @Column(name = "rows_returned")
    private Integer rowsReturned;

    @Column(name = "was_successful")
    private Boolean wasSuccessful;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "attempt_number")
    private Integer attemptNumber = 1;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private EduUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();
    }

    /**
     * Check if query executed successfully
     */
    public boolean isSuccessful() {
        return Boolean.TRUE.equals(wasSuccessful);
    }

    /**
     * Check if query returned the correct result
     */
    public boolean isCorrectResult() {
        return Boolean.TRUE.equals(isCorrect);
    }

    /**
     * Get execution time in seconds
     */
    public Double getExecutionTimeSeconds() {
        if (executionTimeMs == null) return null;
        return executionTimeMs / 1000.0;
    }

    /**
     * Get status emoji
     */
    public String getStatusEmoji() {
        if (Boolean.TRUE.equals(isCorrect)) return "✓";
        if (Boolean.TRUE.equals(wasSuccessful)) return "⚠";
        return "✗";
    }

    /**
     * Get status label
     */
    public String getStatusLabel() {
        if (Boolean.TRUE.equals(isCorrect)) return "Correct";
        if (Boolean.TRUE.equals(wasSuccessful)) return "Executed but incorrect";
        return "Failed";
    }
}