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
 * Repository for EduUser entity
 */
@Repository
public interface EduUserRepository extends JpaRepository<EduUser, Long> {

    // Find by username
    Optional<EduUser> findByUsername(String username);

    // Find by email
    Optional<EduUser> findByEmail(String email);

    // Find active students
    List<EduUser> findByRoleAndIsActiveTrue(EduUser.Role role);

    // Check if username exists
    boolean existsByUsername(String username);

    // Check if email exists
    boolean existsByEmail(String email);
}
