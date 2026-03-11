package com.opensensemap.edu.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * UserProgress Entity - Tracks a student's progress through a scenario
 * Maps to the 'user_progress' table in the database
 */
@Entity
@Table(name = "user_progress")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", insertable = false, updatable = false)
    private Long userId;

    @Column(name = "scenario_id", insertable = false, updatable = false)
    private Long scenarioId;

    @Column(name = "tasks_completed")
    private Integer tasksCompleted = 0;

    @Column(name = "total_tasks")
    private Integer totalTasks;

    @Column(name = "score")
    private Integer score = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private ProgressStatus status = ProgressStatus.IN_PROGRESS;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "last_activity")
    private LocalDateTime lastActivity;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private EduUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", nullable = false)
    private Scenario scenario;

    // Enum for progress status
    public enum ProgressStatus {
        NOT_STARTED, IN_PROGRESS, COMPLETED, ABANDONED
    }

    @PrePersist
    protected void onCreate() {
        startedAt = LocalDateTime.now();
        lastActivity = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastActivity = LocalDateTime.now();
    }

    /**
     * Calculate completion percentage
     */
    public int getCompletionPercentage() {
        if (totalTasks == null || totalTasks == 0) return 0;
        if (tasksCompleted == null) return 0;
        return (int) ((tasksCompleted * 100.0) / totalTasks);
    }

    /**
     * Check if scenario is completed
     */
    public boolean isCompleted() {
        return status == ProgressStatus.COMPLETED;
    }

    /**
     * Check if scenario is in progress
     */
    public boolean isInProgress() {
        return status == ProgressStatus.IN_PROGRESS;
    }

    /**
     * Mark task as completed
     */
    public void completeTask(int pointsEarned) {
        if (tasksCompleted == null) tasksCompleted = 0;
        if (score == null) score = 0;

        tasksCompleted++;
        score += pointsEarned;
        lastActivity = LocalDateTime.now();

        // Check if all tasks completed
        if (totalTasks != null && tasksCompleted >= totalTasks) {
            status = ProgressStatus.COMPLETED;
            completedAt = LocalDateTime.now();
        }
    }

    /**
     * Get progress bar representation
     */
    public String getProgressBar(int width) {
        int percentage = getCompletionPercentage();
        int filled = (int) ((percentage / 100.0) * width);
        int empty = width - filled;

        return "█".repeat(Math.max(0, filled)) +
                "░".repeat(Math.max(0, empty)) +
                " " + percentage + "%";
    }
}