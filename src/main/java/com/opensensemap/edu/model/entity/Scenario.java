package com.opensensemap.edu.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Scenario Entity - Represents a story-based learning scenario
 * Maps to the 'scenario' table in the database
 */
@Entity
@Table(name = "scenario")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Scenario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "story_context", columnDefinition = "TEXT")
    private String storyContext;

    @Column(name = "role", length = 100)
    private String role;

    @Column(name = "difficulty_level")
    private Integer difficultyLevel;

    @Column(name = "learning_objectives")
    private String[] learningObjectives;

    @Column(name = "estimated_time_minutes")
    private Integer estimatedTimeMinutes;

    @Column(name = "icon", length = 50)
    private String icon;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_published")
    private Boolean isPublished = false;

    @Column(name = "order_index")
    private Integer orderIndex = 0;

    // Relationships
    @OneToMany(mappedBy = "scenario", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("taskOrder ASC")
    private List<Task> tasks = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Get difficulty as string (Beginner, Intermediate, Advanced, Expert, Master)
     */
    public String getDifficultyLabel() {
        if (difficultyLevel == null) return "Unknown";
        return switch (difficultyLevel) {
            case 1 -> "Beginner";
            case 2 -> "Intermediate";
            case 3 -> "Advanced";
            case 4 -> "Expert";
            case 5 -> "Master";
            default -> "Unknown";
        };
    }

    /**
     * Get total number of tasks
     */
    public int getTotalTasks() {
        return tasks != null ? tasks.size() : 0;
    }

    /**
     * Get total possible points
     */
    public int getTotalPoints() {
        if (tasks == null || tasks.isEmpty()) return 0;
        return tasks.stream()
                .mapToInt(Task::getPoints)
                .sum();
    }

    /**
     * Check if scenario is ready for students
     */
    public boolean isReady() {
        return isPublished && tasks != null && !tasks.isEmpty();
    }
}