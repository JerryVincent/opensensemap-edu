package com.opensensemap.edu.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Task Entity - Represents an individual SQL task within a scenario
 * Maps to the 'task' table in the database
 */
@Entity
@Table(name = "task")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scenario_id", insertable = false, updatable = false)
    private Long scenarioId;

    @Column(name = "task_order", nullable = false)
    private Integer taskOrder;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "hint", columnDefinition = "TEXT")
    private String hint;

    @Column(name = "sql_concepts")
    private String[] sqlConcepts;

    @Column(name = "expected_result_description", columnDefinition = "TEXT")
    private String expectedResultDescription;

    @Column(name = "sample_solution", columnDefinition = "TEXT")
    private String sampleSolution;

    @Column(name = "validation_query", columnDefinition = "TEXT")
    private String validationQuery;

    @Column(name = "points")
    private Integer points = 10;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", nullable = false)
    private Scenario scenario;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Check if task has a hint
     */
    public boolean hasHint() {
        return hint != null && !hint.trim().isEmpty();
    }

    /**
     * Check if task has validation query
     */
    public boolean hasValidation() {
        return validationQuery != null && !validationQuery.trim().isEmpty();
    }

    /**
     * Get SQL concepts as comma-separated string
     */
    public String getSqlConceptsString() {
        if (sqlConcepts == null || sqlConcepts.length == 0) {
            return "";
        }
        return String.join(", ", sqlConcepts);
    }

    /**
     * Get difficulty based on task order (later tasks are harder)
     */
    public String getDifficultyLabel() {
        if (taskOrder == null) return "Unknown";
        if (taskOrder <= 3) return "Easy";
        if (taskOrder <= 6) return "Medium";
        return "Hard";
    }
}