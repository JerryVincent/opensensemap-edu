package com.opensensemap.edu.controller;

import com.opensensemap.edu.model.entity.EduUser;
import com.opensensemap.edu.model.entity.Scenario;
import com.opensensemap.edu.model.entity.Task;
import com.opensensemap.edu.repository.EduUserRepository;
import com.opensensemap.edu.repository.QuerySubmissionRepository;
import com.opensensemap.edu.repository.ScenarioRepository;
import com.opensensemap.edu.repository.TaskRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.*;

/**
 * HomeController - Handles web pages for the application
 */
@Controller
public class HomeController {

    private final ScenarioRepository scenarioRepository;
    private final TaskRepository taskRepository;
    private final EduUserRepository userRepository;
    private final QuerySubmissionRepository submissionRepository;

    public HomeController(ScenarioRepository scenarioRepository,
                          TaskRepository taskRepository,
                          EduUserRepository userRepository,
                          QuerySubmissionRepository submissionRepository) {
        this.scenarioRepository = scenarioRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.submissionRepository = submissionRepository;
    }

    /**
     * Homepage - Welcome page with quick links
     */
    @GetMapping("/")
    public String home(Authentication authentication, Model model) {
        List<Scenario> scenarios = scenarioRepository.findByIsPublishedTrueOrderByOrderIndexAsc();

        model.addAttribute("title", "OpenSenseMap Educational Tool");
        model.addAttribute("scenarios", scenarios);

        addUserContext(authentication, model);
        addScenarioProgress(authentication, scenarios, model);

        return "home";
    }

    /**
     * Scenarios list page
     */
    @GetMapping("/scenarios")
    public String scenariosList(Authentication authentication, Model model) {
        List<Scenario> scenarios = scenarioRepository.findByIsPublishedTrueOrderByOrderIndexAsc();

        model.addAttribute("title", "Learning Scenarios");
        model.addAttribute("scenarios", scenarios);

        addUserContext(authentication, model);
        addScenarioProgress(authentication, scenarios, model);

        return "scenarios/list";
    }

    /**
     * Single scenario page with tasks
     */
    @GetMapping("/scenarios/{id}")
    public String scenarioDetail(@PathVariable Long id, Authentication authentication, Model model) {
        Scenario scenario = scenarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Scenario not found"));

        List<Task> tasks = taskRepository.findByScenarioIdOrderByTaskOrderAsc(id);

        model.addAttribute("scenario", scenario);
        model.addAttribute("tasks", tasks);
        model.addAttribute("title", scenario.getTitle());

        addUserContext(authentication, model);

        // Add per-task completion for logged-in users
        EduUser user = getAuthenticatedUser(authentication);
        if (user != null) {
            Set<Long> completedTaskIds = getCompletedTaskIds(user.getId(), tasks);
            model.addAttribute("completedTaskIds", completedTaskIds);
        } else {
            model.addAttribute("completedTaskIds", Collections.emptySet());
        }

        return "scenarios/detail";
    }

    /**
     * Task workspace - where students write queries
     */
    @GetMapping("/scenarios/{scenarioId}/task/{taskId}")
    public String taskWorkspace(
            @PathVariable Long scenarioId,
            @PathVariable Long taskId,
            Authentication authentication,
            Model model) {

        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new RuntimeException("Scenario not found"));

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        List<Task> allTasks = taskRepository.findByScenarioIdOrderByTaskOrderAsc(scenarioId);

        model.addAttribute("scenario", scenario);
        model.addAttribute("task", task);
        model.addAttribute("allTasks", allTasks);
        model.addAttribute("title", task.getTitle());

        EduUser user = getAuthenticatedUser(authentication);
        if (user != null) {
            model.addAttribute("currentUserId", user.getId());
            model.addAttribute("isLoggedIn", true);

            // Add completed task IDs for sidebar checkmarks
            Set<Long> completedTaskIds = getCompletedTaskIds(user.getId(), allTasks);
            model.addAttribute("completedTaskIds", completedTaskIds);
        } else {
            model.addAttribute("currentUserId", null);
            model.addAttribute("isLoggedIn", false);
            model.addAttribute("completedTaskIds", Collections.emptySet());
        }

        return "task/workspace";
    }

    /**
     * SQL Playground - Free-form SQL practice area
     */
    @GetMapping("/playground")
    public String sqlPlayground(Authentication authentication, Model model) {
        model.addAttribute("title", "SQL Playground");
        addUserContext(authentication, model);
        return "playground";
    }

    // ================================================================
    // HELPER METHODS
    // ================================================================

    private EduUser getAuthenticatedUser(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getName().equals("anonymousUser")) {
            return userRepository.findByUsername(authentication.getName()).orElse(null);
        }
        return null;
    }

    private void addUserContext(Authentication authentication, Model model) {
        EduUser user = getAuthenticatedUser(authentication);
        if (user != null) {
            model.addAttribute("currentUserId", user.getId());
            model.addAttribute("isLoggedIn", true);
            model.addAttribute("username", user.getUsername());
        } else {
            model.addAttribute("currentUserId", null);
            model.addAttribute("isLoggedIn", false);
        }
    }

    /**
     * Get set of task IDs that the user has correctly completed
     */
    private Set<Long> getCompletedTaskIds(Long userId, List<Task> tasks) {
        Set<Long> completedTaskIds = new HashSet<>();
        for (Task task : tasks) {
            long correctCount = submissionRepository.countCorrectByUserAndTask(userId, task.getId());
            if (correctCount > 0) {
                completedTaskIds.add(task.getId());
            }
        }
        return completedTaskIds;
    }

    /**
     * Add per-scenario progress maps to the model.
     * Keys: completedTaskCounts, totalTaskCounts, scenarioStatuses
     */
    private void addScenarioProgress(Authentication authentication, List<Scenario> scenarios, Model model) {
        EduUser user = getAuthenticatedUser(authentication);

        Map<Long, Integer> completedTaskCounts = new HashMap<>();
        Map<Long, Integer> totalTaskCounts = new HashMap<>();
        Map<Long, String> scenarioStatuses = new HashMap<>();

        for (Scenario scenario : scenarios) {
            List<Task> tasks = taskRepository.findByScenarioIdOrderByTaskOrderAsc(scenario.getId());
            int total = tasks.size();
            int completed = 0;

            if (user != null) {
                completed = getCompletedTaskIds(user.getId(), tasks).size();
            }

            totalTaskCounts.put(scenario.getId(), total);
            completedTaskCounts.put(scenario.getId(), completed);

            if (completed == 0) {
                scenarioStatuses.put(scenario.getId(), "not_started");
            } else if (completed >= total) {
                scenarioStatuses.put(scenario.getId(), "completed");
            } else {
                scenarioStatuses.put(scenario.getId(), "in_progress");
            }
        }

        model.addAttribute("completedTaskCounts", completedTaskCounts);
        model.addAttribute("totalTaskCounts", totalTaskCounts);
        model.addAttribute("scenarioStatuses", scenarioStatuses);
    }
}