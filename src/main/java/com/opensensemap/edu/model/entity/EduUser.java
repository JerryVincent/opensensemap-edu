package com.opensensemap.edu.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * EduUser Entity - Represents students and teachers in the educational platform
 * Maps to the 'edu_user' table in the database
 */
@Entity
@Table(name = "edu_user")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EduUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;

    @Column(name = "email", unique = true, nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20)
    private Role role = Role.STUDENT;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "is_active")
    private Boolean isActive = true;

    // Relationships
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserProgress> progressList = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<QuerySubmission> querySubmissions = new ArrayList<>();

    // Enum for user roles
    public enum Role {
        STUDENT, TEACHER, ADMIN
    }

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
     * Check if user is a student
     */
    public boolean isStudent() {
        return role == Role.STUDENT;
    }

    /**
     * Check if user is a teacher
     */
    public boolean isTeacher() {
        return role == Role.TEACHER;
    }

    /**
     * Check if user is an admin
     */
    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    /**
     * Update last login time
     */
    public void updateLastLogin() {
        this.lastLogin = LocalDateTime.now();
    }
}