package com.opensensemap.edu.service;

import com.opensensemap.edu.model.entity.Scenario;
import com.opensensemap.edu.model.entity.Task;
import com.opensensemap.edu.repository.ScenarioRepository;
import com.opensensemap.edu.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for scenario and task management
 */
@Service
public class ScenarioService {

    private final ScenarioRepository scenarioRepository;
    private final TaskRepository taskRepository;

    public ScenarioService(ScenarioRepository scenarioRepository, TaskRepository taskRepository) {
        this.scenarioRepository = scenarioRepository;
        this.taskRepository = taskRepository;
    }

    // ============ Scenario Operations ============

    /**
     * Get all scenarios (for admin)
     */
    public List<Scenario> getAllScenarios() {
        return scenarioRepository.findAll();
    }

    /**
     * Get published scenarios (for students)
     */
    public List<Scenario> getPublishedScenarios() {
        return scenarioRepository.findByIsPublishedTrueOrderByOrderIndexAsc();
    }

    /**
     * Get scenario by ID
     */
    public Optional<Scenario> getScenarioById(Long id) {
        return scenarioRepository.findById(id);
    }

    /**
     * Create new scenario
     */
    @Transactional
    public Scenario createScenario(Scenario scenario) {
        // Set default order index if not provided
        if (scenario.getOrderIndex() == null) {
            long count = scenarioRepository.count();
            scenario.setOrderIndex((int) count + 1);
        }
        return scenarioRepository.save(scenario);
    }

    /**
     * Update existing scenario
     */
    @Transactional
    public Scenario updateScenario(Long id, Scenario updatedScenario) {
        Scenario existing = scenarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Scenario not found"));

        existing.setTitle(updatedScenario.getTitle());
        existing.setDescription(updatedScenario.getDescription());
        existing.setStoryContext(updatedScenario.getStoryContext());
        existing.setRole(updatedScenario.getRole());
        existing.setDifficultyLevel(updatedScenario.getDifficultyLevel());
        existing.setLearningObjectives(updatedScenario.getLearningObjectives());
        existing.setEstimatedTimeMinutes(updatedScenario.getEstimatedTimeMinutes());
        existing.setIcon(updatedScenario.getIcon());
        existing.setIsPublished(updatedScenario.getIsPublished());
        existing.setOrderIndex(updatedScenario.getOrderIndex());

        return scenarioRepository.save(existing);
    }

    /**
     * Delete scenario
     */
    @Transactional
    public void deleteScenario(Long id) {
        scenarioRepository.deleteById(id);
    }

    /**
     * Toggle scenario published status
     */
    @Transactional
    public Scenario togglePublished(Long id) {
        Scenario scenario = scenarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Scenario not found"));
        scenario.setIsPublished(!scenario.getIsPublished());
        return scenarioRepository.save(scenario);
    }

    /**
     * Count total scenarios
     */
    public long countScenarios() {
        return scenarioRepository.count();
    }

    /**
     * Count published scenarios
     */
    public long countPublishedScenarios() {
        return scenarioRepository.findByIsPublishedTrueOrderByOrderIndexAsc().size();
    }

    // ============ Task Operations ============

    /**
     * Get all tasks for a scenario
     */
    public List<Task> getTasksForScenario(Long scenarioId) {
        return taskRepository.findByScenarioIdOrderByTaskOrderAsc(scenarioId);
    }

    /**
     * Get task by ID
     */
    public Optional<Task> getTaskById(Long id) {
        return taskRepository.findById(id);
    }

    /**
     * Create new task
     */
    @Transactional
    public Task createTask(Task task, Long scenarioId) {
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new RuntimeException("Scenario not found"));

        task.setScenario(scenario);

        // Set task order if not provided
        if (task.getTaskOrder() == null) {
            long count = taskRepository.countByScenarioId(scenarioId);
            task.setTaskOrder((int) count + 1);
        }

        return taskRepository.save(task);
    }

    /**
     * Update existing task
     */
    @Transactional
    public Task updateTask(Long id, Task updatedTask) {
        Task existing = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        existing.setTitle(updatedTask.getTitle());
        existing.setDescription(updatedTask.getDescription());
        existing.setHint(updatedTask.getHint());
        existing.setSqlConcepts(updatedTask.getSqlConcepts());
        existing.setExpectedResultDescription(updatedTask.getExpectedResultDescription());
        existing.setSampleSolution(updatedTask.getSampleSolution());
        existing.setValidationQuery(updatedTask.getValidationQuery());
        existing.setPoints(updatedTask.getPoints());
        existing.setTaskOrder(updatedTask.getTaskOrder());

        return taskRepository.save(existing);
    }

    /**
     * Delete task
     */
    @Transactional
    public void deleteTask(Long id) {
        taskRepository.deleteById(id);
    }

    /**
     * Count total tasks
     */
    public long countTasks() {
        return taskRepository.count();
    }

    /**
     * Reorder tasks in a scenario
     */
    @Transactional
    public void reorderTasks(Long scenarioId, List<Long> taskIds) {
        int order = 1;
        for (Long taskId : taskIds) {
            Task task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new RuntimeException("Task not found"));
            task.setTaskOrder(order++);
            taskRepository.save(task);
        }
    }
}
