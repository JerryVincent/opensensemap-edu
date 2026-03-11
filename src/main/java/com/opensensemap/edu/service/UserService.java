package com.opensensemap.edu.service;

import com.opensensemap.edu.model.entity.EduUser;
import com.opensensemap.edu.model.entity.Scenario;
import com.opensensemap.edu.model.entity.UserProgress;
import com.opensensemap.edu.repository.EduUserRepository;
import com.opensensemap.edu.repository.QuerySubmissionRepository;
import com.opensensemap.edu.repository.ScenarioRepository;
import com.opensensemap.edu.repository.UserProgressRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for user management operations
 */
@Service
public class UserService {

    private final EduUserRepository userRepository;
    private final UserProgressRepository progressRepository;
    private final QuerySubmissionRepository submissionRepository;
    private final ScenarioRepository scenarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(EduUserRepository userRepository,
                       UserProgressRepository progressRepository,
                       QuerySubmissionRepository submissionRepository,
                       ScenarioRepository scenarioRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.progressRepository = progressRepository;
        this.submissionRepository = submissionRepository;
        this.scenarioRepository = scenarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Register a new user
     */
    @Transactional
    public EduUser registerUser(String username, String email, String password, String fullName) {
        // Check if username or email already exists
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        EduUser user = new EduUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setRole(EduUser.Role.STUDENT);
        user.setIsActive(true);

        return userRepository.save(user);
    }

    /**
     * Get user by username
     */
    public Optional<EduUser> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Get user by ID
     */
    public Optional<EduUser> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Update last login time
     */
    @Transactional
    public void updateLastLogin(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    /**
     * Get all students
     */
    public List<EduUser> getAllStudents() {
        return userRepository.findByRoleAndIsActiveTrue(EduUser.Role.STUDENT);
    }

    /**
     * Get all users
     */
    public List<EduUser> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Get user progress across all scenarios
     */
    public List<UserProgress> getUserProgress(Long userId) {
        return progressRepository.findByUserIdOrderByLastActivityDesc(userId);
    }

    /**
     * Get or create progress for a scenario
     */
    @Transactional
    public UserProgress getOrCreateProgress(Long userId, Long scenarioId) {
        return progressRepository.findByUserIdAndScenarioId(userId, scenarioId)
                .orElseGet(() -> {
                    EduUser user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User not found"));
                    Scenario scenario = scenarioRepository.findById(scenarioId)
                            .orElseThrow(() -> new RuntimeException("Scenario not found"));

                    UserProgress progress = new UserProgress();
                    progress.setUser(user);
                    progress.setScenario(scenario);
                    progress.setTotalTasks(scenario.getTotalTasks());
                    progress.setTasksCompleted(0);
                    progress.setScore(0);
                    progress.setStatus(UserProgress.ProgressStatus.IN_PROGRESS);

                    return progressRepository.save(progress);
                });
    }

    /**
     * Calculate total score for a user
     */
    public int getTotalScore(Long userId) {
        Integer score = progressRepository.calculateTotalScore(userId);
        return score != null ? score : 0;
    }

    /**
     * Count completed tasks for a user
     */
    public long getCompletedTasksCount(Long userId) {
        return submissionRepository.countCorrectSubmissionsByUser(userId);
    }

    /**
     * Get in-progress scenarios for a user
     */
    public List<UserProgress> getInProgressScenarios(Long userId) {
        return progressRepository.findInProgressScenarios(userId);
    }

    /**
     * Get completed scenarios for a user
     */
    public List<UserProgress> getCompletedScenarios(Long userId) {
        return progressRepository.findByUserIdAndStatus(userId, UserProgress.ProgressStatus.COMPLETED);
    }

    /**
     * Count users by role
     */
    public long countByRole(EduUser.Role role) {
        return userRepository.findByRoleAndIsActiveTrue(role).size();
    }

    /**
     * Create admin user if not exists (for initial setup)
     */
    @Transactional
    public void createDefaultAdminIfNotExists() {
        if (!userRepository.existsByUsername("admin")) {
            EduUser admin = new EduUser();
            admin.setUsername("admin");
            admin.setEmail("admin@opensensemap.edu");
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setFullName("Administrator");
            admin.setRole(EduUser.Role.ADMIN);
            admin.setIsActive(true);
            userRepository.save(admin);
        }
    }

    /**
     * Create demo student if not exists
     */
    @Transactional
    public void createDemoStudentIfNotExists() {
        if (!userRepository.existsByUsername("student")) {
            EduUser student = new EduUser();
            student.setUsername("student");
            student.setEmail("student@opensensemap.edu");
            student.setPasswordHash(passwordEncoder.encode("student123"));
            student.setFullName("Demo Student");
            student.setRole(EduUser.Role.STUDENT);
            student.setIsActive(true);
            userRepository.save(student);
        }
    }

    /**
     * Create a new user (admin function)
     */
    @Transactional
    public EduUser createUser(String username, String email, String password, String fullName, EduUser.Role role) {
        // Check if username or email already exists
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        EduUser user = new EduUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setRole(role);
        user.setIsActive(true);

        return userRepository.save(user);
    }

    /**
     * Update existing user (admin function)
     */
    @Transactional
    public EduUser updateUser(Long id, String username, String email, String fullName, 
                              EduUser.Role role, Boolean isActive, String newPassword) {
        EduUser user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Check if username is taken by another user
        userRepository.findByUsername(username).ifPresent(existingUser -> {
            if (!existingUser.getId().equals(id)) {
                throw new IllegalArgumentException("Username already exists");
            }
        });

        // Check if email is taken by another user
        userRepository.findByEmail(email).ifPresent(existingUser -> {
            if (!existingUser.getId().equals(id)) {
                throw new IllegalArgumentException("Email already exists");
            }
        });

        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setRole(role);
        user.setIsActive(isActive);

        // Only update password if provided
        if (newPassword != null && !newPassword.trim().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(newPassword));
        }

        return userRepository.save(user);
    }

    /**
     * Toggle user active status
     */
    @Transactional
    public void toggleUserActive(Long id) {
        EduUser user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setIsActive(!user.getIsActive());
        userRepository.save(user);
    }

    /**
     * Delete user and their associated data
     */
    @Transactional
    public void deleteUser(Long id) {
        EduUser user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Delete user's submissions
        submissionRepository.deleteByUserId(id);
        
        // Delete user's progress
        progressRepository.deleteByUserId(id);
        
        // Delete the user
        userRepository.delete(user);
    }
}
