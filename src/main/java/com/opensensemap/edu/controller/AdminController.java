package com.opensensemap.edu.controller;

import com.opensensemap.edu.model.entity.EduUser;
import com.opensensemap.edu.model.entity.Scenario;
import com.opensensemap.edu.model.entity.Task;
import com.opensensemap.edu.repository.QuerySubmissionRepository;
import com.opensensemap.edu.service.ScenarioService;
import com.opensensemap.edu.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;

/**
 * Admin Controller
 * Handles admin dashboard, scenario management, and task management
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ScenarioService scenarioService;
    private final UserService userService;
    private final QuerySubmissionRepository submissionRepository;

    public AdminController(ScenarioService scenarioService,
                           UserService userService,
                           QuerySubmissionRepository submissionRepository) {
        this.scenarioService = scenarioService;
        this.userService = userService;
        this.submissionRepository = submissionRepository;
    }

    /**
     * Admin Dashboard
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Statistics
        long totalScenarios = scenarioService.countScenarios();
        long publishedScenarios = scenarioService.countPublishedScenarios();
        long totalTasks = scenarioService.countTasks();
        long totalStudents = userService.countByRole(EduUser.Role.STUDENT);
        long totalSubmissions = submissionRepository.count();

        model.addAttribute("totalScenarios", totalScenarios);
        model.addAttribute("publishedScenarios", publishedScenarios);
        model.addAttribute("totalTasks", totalTasks);
        model.addAttribute("totalStudents", totalStudents);
        model.addAttribute("totalSubmissions", totalSubmissions);

        // Recent scenarios
        List<Scenario> recentScenarios = scenarioService.getAllScenarios();
        model.addAttribute("scenarios", recentScenarios);

        // Recent students
        List<EduUser> recentStudents = userService.getAllStudents();
        model.addAttribute("students", recentStudents);

        model.addAttribute("title", "Admin Dashboard");
        return "admin/dashboard";
    }

    // ============ Scenario Management ============

    /**
     * List all scenarios
     */
    @GetMapping("/scenarios")
    public String listScenarios(Model model) {
        List<Scenario> scenarios = scenarioService.getAllScenarios();
        model.addAttribute("scenarios", scenarios);
        model.addAttribute("title", "Manage Scenarios");
        return "admin/scenarios/list";
    }

    /**
     * Create scenario form
     */
    @GetMapping("/scenarios/new")
    public String newScenarioForm(Model model) {
        model.addAttribute("scenario", new Scenario());
        model.addAttribute("title", "Create Scenario");
        model.addAttribute("isEdit", false);
        return "admin/scenarios/form";
    }

    /**
     * Edit scenario form
     */
    @GetMapping("/scenarios/{id}/edit")
    public String editScenarioForm(@PathVariable Long id, Model model) {
        Scenario scenario = scenarioService.getScenarioById(id)
                .orElseThrow(() -> new RuntimeException("Scenario not found"));

        model.addAttribute("scenario", scenario);
        model.addAttribute("title", "Edit Scenario");
        model.addAttribute("isEdit", true);
        return "admin/scenarios/form";
    }

    /**
     * Save scenario (create or update)
     */
    @PostMapping("/scenarios/save")
    public String saveScenario(@Valid @ModelAttribute("scenario") Scenario scenario,
                               BindingResult result,
                               @RequestParam(required = false) String learningObjectivesText,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("title", scenario.getId() == null ? "Create Scenario" : "Edit Scenario");
            model.addAttribute("isEdit", scenario.getId() != null);
            return "admin/scenarios/form";
        }

        // Parse learning objectives from textarea
        if (learningObjectivesText != null && !learningObjectivesText.trim().isEmpty()) {
            String[] objectives = learningObjectivesText.split("\\r?\\n");
            scenario.setLearningObjectives(objectives);
        }

        if (scenario.getId() == null) {
            scenarioService.createScenario(scenario);
            redirectAttributes.addFlashAttribute("message", "Scenario created successfully!");
        } else {
            scenarioService.updateScenario(scenario.getId(), scenario);
            redirectAttributes.addFlashAttribute("message", "Scenario updated successfully!");
        }

        return "redirect:/admin/scenarios";
    }

    /**
     * Delete scenario
     */
    @PostMapping("/scenarios/{id}/delete")
    public String deleteScenario(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        scenarioService.deleteScenario(id);
        redirectAttributes.addFlashAttribute("message", "Scenario deleted successfully!");
        return "redirect:/admin/scenarios";
    }

    /**
     * Toggle scenario published status
     */
    @PostMapping("/scenarios/{id}/toggle-publish")
    public String togglePublish(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Scenario scenario = scenarioService.togglePublished(id);
        String status = scenario.getIsPublished() ? "published" : "unpublished";
        redirectAttributes.addFlashAttribute("message", "Scenario " + status + " successfully!");
        return "redirect:/admin/scenarios";
    }

    // ============ Task Management ============

    /**
     * View tasks for a scenario
     */
    @GetMapping("/scenarios/{scenarioId}/tasks")
    public String listTasks(@PathVariable Long scenarioId, Model model) {
        Scenario scenario = scenarioService.getScenarioById(scenarioId)
                .orElseThrow(() -> new RuntimeException("Scenario not found"));

        List<Task> tasks = scenarioService.getTasksForScenario(scenarioId);

        model.addAttribute("scenario", scenario);
        model.addAttribute("tasks", tasks);
        model.addAttribute("title", "Tasks - " + scenario.getTitle());
        return "admin/tasks/list";
    }

    /**
     * Create task form
     */
    @GetMapping("/scenarios/{scenarioId}/tasks/new")
    public String newTaskForm(@PathVariable Long scenarioId, Model model) {
        Scenario scenario = scenarioService.getScenarioById(scenarioId)
                .orElseThrow(() -> new RuntimeException("Scenario not found"));

        Task task = new Task();
        task.setPoints(10); // Default points

        model.addAttribute("scenario", scenario);
        model.addAttribute("task", task);
        model.addAttribute("title", "Create Task");
        model.addAttribute("isEdit", false);
        return "admin/tasks/form";
    }

    /**
     * Edit task form
     */
    @GetMapping("/scenarios/{scenarioId}/tasks/{taskId}/edit")
    public String editTaskForm(@PathVariable Long scenarioId,
                               @PathVariable Long taskId,
                               Model model) {
        Scenario scenario = scenarioService.getScenarioById(scenarioId)
                .orElseThrow(() -> new RuntimeException("Scenario not found"));

        Task task = scenarioService.getTaskById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        model.addAttribute("scenario", scenario);
        model.addAttribute("task", task);
        model.addAttribute("title", "Edit Task");
        model.addAttribute("isEdit", true);
        return "admin/tasks/form";
    }

    /**
     * Save task (create or update)
     */
    @PostMapping("/scenarios/{scenarioId}/tasks/save")
    public String saveTask(@PathVariable Long scenarioId,
                           @Valid @ModelAttribute("task") Task task,
                           BindingResult result,
                           @RequestParam(required = false) String sqlConceptsText,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        
        Scenario scenario = scenarioService.getScenarioById(scenarioId)
                .orElseThrow(() -> new RuntimeException("Scenario not found"));

        if (result.hasErrors()) {
            model.addAttribute("scenario", scenario);
            model.addAttribute("title", task.getId() == null ? "Create Task" : "Edit Task");
            model.addAttribute("isEdit", task.getId() != null);
            return "admin/tasks/form";
        }

        // Parse SQL concepts from comma-separated text
        if (sqlConceptsText != null && !sqlConceptsText.trim().isEmpty()) {
            String[] concepts = Arrays.stream(sqlConceptsText.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toArray(String[]::new);
            task.setSqlConcepts(concepts);
        }

        if (task.getId() == null) {
            scenarioService.createTask(task, scenarioId);
            redirectAttributes.addFlashAttribute("message", "Task created successfully!");
        } else {
            scenarioService.updateTask(task.getId(), task);
            redirectAttributes.addFlashAttribute("message", "Task updated successfully!");
        }

        return "redirect:/admin/scenarios/" + scenarioId + "/tasks";
    }

    /**
     * Delete task
     */
    @PostMapping("/scenarios/{scenarioId}/tasks/{taskId}/delete")
    public String deleteTask(@PathVariable Long scenarioId,
                             @PathVariable Long taskId,
                             RedirectAttributes redirectAttributes) {
        scenarioService.deleteTask(taskId);
        redirectAttributes.addFlashAttribute("message", "Task deleted successfully!");
        return "redirect:/admin/scenarios/" + scenarioId + "/tasks";
    }

    // ============ User Management ============

    /**
     * List all users
     */
    @GetMapping("/users")
    public String listUsers(Model model) {
        List<EduUser> users = userService.getAllUsers();
        
        // Count by role
        long studentCount = users.stream().filter(u -> u.getRole() == EduUser.Role.STUDENT).count();
        long teacherCount = users.stream().filter(u -> u.getRole() == EduUser.Role.TEACHER).count();
        long adminCount = users.stream().filter(u -> u.getRole() == EduUser.Role.ADMIN).count();
        
        model.addAttribute("users", users);
        model.addAttribute("studentCount", studentCount);
        model.addAttribute("teacherCount", teacherCount);
        model.addAttribute("adminCount", adminCount);
        model.addAttribute("title", "Manage Users");
        return "admin/users/list";
    }

    /**
     * Show create user form
     */
    @GetMapping("/users/new")
    public String newUserForm(Model model) {
        model.addAttribute("user", new EduUser());
        model.addAttribute("roles", EduUser.Role.values());
        model.addAttribute("title", "Create User");
        model.addAttribute("isEdit", false);
        return "admin/users/form";
    }

    /**
     * Show edit user form
     */
    @GetMapping("/users/{id}/edit")
    public String editUserForm(@PathVariable Long id, Model model) {
        EduUser user = userService.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", user);
        model.addAttribute("roles", EduUser.Role.values());
        model.addAttribute("title", "Edit User");
        model.addAttribute("isEdit", true);
        return "admin/users/form";
    }

    /**
     * Save user (create or update)
     */
    @PostMapping("/users/save")
    public String saveUser(@ModelAttribute EduUser user,
                           @RequestParam(required = false) String newPassword,
                           @RequestParam(required = false) boolean isEdit,
                           RedirectAttributes redirectAttributes) {
        try {
            if (isEdit) {
                // Update existing user
                userService.updateUser(user.getId(), user.getUsername(), user.getEmail(), 
                                       user.getFullName(), user.getRole(), user.getIsActive(), newPassword);
                redirectAttributes.addFlashAttribute("message", "User updated successfully!");
            } else {
                // Create new user
                if (newPassword == null || newPassword.trim().isEmpty()) {
                    redirectAttributes.addFlashAttribute("error", "Password is required for new users");
                    return "redirect:/admin/users/new";
                }
                userService.createUser(user.getUsername(), user.getEmail(), newPassword, 
                                       user.getFullName(), user.getRole());
                redirectAttributes.addFlashAttribute("message", "User created successfully!");
            }
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            if (isEdit) {
                return "redirect:/admin/users/" + user.getId() + "/edit";
            }
            return "redirect:/admin/users/new";
        }
        return "redirect:/admin/users";
    }

    /**
     * Toggle user active status
     */
    @PostMapping("/users/{id}/toggle-active")
    public String toggleUserActive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userService.toggleUserActive(id);
        redirectAttributes.addFlashAttribute("message", "User status updated!");
        return "redirect:/admin/users";
    }

    /**
     * Delete user
     */
    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("message", "User deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Cannot delete user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }
}
