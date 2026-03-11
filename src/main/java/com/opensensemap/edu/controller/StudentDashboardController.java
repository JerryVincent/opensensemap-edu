package com.opensensemap.edu.controller;

import com.opensensemap.edu.model.entity.EduUser;
import com.opensensemap.edu.model.entity.Scenario;
import com.opensensemap.edu.model.entity.UserProgress;
import com.opensensemap.edu.repository.QuerySubmissionRepository;
import com.opensensemap.edu.service.ScenarioService;
import com.opensensemap.edu.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Student Dashboard Controller
 * Handles student dashboard and progress tracking
 */
@Controller
@RequestMapping("/dashboard")
public class StudentDashboardController {

    private final UserService userService;
    private final ScenarioService scenarioService;
    private final QuerySubmissionRepository submissionRepository;

    public StudentDashboardController(UserService userService,
                                       ScenarioService scenarioService,
                                       QuerySubmissionRepository submissionRepository) {
        this.userService = userService;
        this.scenarioService = scenarioService;
        this.submissionRepository = submissionRepository;
    }

    /**
     * Student Dashboard
     */
    @GetMapping
    public String dashboard(Authentication authentication, Model model) {
        String username = authentication.getName();
        EduUser user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Get user statistics
        int totalScore = userService.getTotalScore(user.getId());
        long completedTasks = userService.getCompletedTasksCount(user.getId());
        
        // Get progress across scenarios
        List<UserProgress> allProgress = userService.getUserProgress(user.getId());
        List<UserProgress> inProgressScenarios = userService.getInProgressScenarios(user.getId());
        List<UserProgress> completedScenarios = userService.getCompletedScenarios(user.getId());

        // Get available scenarios
        List<Scenario> availableScenarios = scenarioService.getPublishedScenarios();

        // Calculate overall stats
        long totalSubmissions = submissionRepository.findByUserIdOrderBySubmittedAtDesc(user.getId()).size();

        model.addAttribute("user", user);
        model.addAttribute("totalScore", totalScore);
        model.addAttribute("completedTasks", completedTasks);
        model.addAttribute("totalSubmissions", totalSubmissions);
        model.addAttribute("allProgress", allProgress);
        model.addAttribute("inProgressScenarios", inProgressScenarios);
        model.addAttribute("completedScenarios", completedScenarios);
        model.addAttribute("availableScenarios", availableScenarios);
        model.addAttribute("title", "My Dashboard");

        return "student/dashboard";
    }

    /**
     * View progress for a specific scenario
     */
    @GetMapping("/scenario/{scenarioId}")
    public String scenarioProgress(@PathVariable Long scenarioId,
                                   Authentication authentication,
                                   Model model) {
        String username = authentication.getName();
        EduUser user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Scenario scenario = scenarioService.getScenarioById(scenarioId)
                .orElseThrow(() -> new RuntimeException("Scenario not found"));

        // Get or create progress
        UserProgress progress = userService.getOrCreateProgress(user.getId(), scenarioId);

        // Get submissions for this scenario's tasks
        var tasks = scenarioService.getTasksForScenario(scenarioId);
        var submissions = submissionRepository.findByUserIdOrderBySubmittedAtDesc(user.getId());

        model.addAttribute("user", user);
        model.addAttribute("scenario", scenario);
        model.addAttribute("progress", progress);
        model.addAttribute("tasks", tasks);
        model.addAttribute("submissions", submissions);
        model.addAttribute("title", scenario.getTitle() + " - Progress");

        return "student/scenario-progress";
    }

    /**
     * View submission history
     */
    @GetMapping("/history")
    public String submissionHistory(Authentication authentication, Model model) {
        String username = authentication.getName();
        EduUser user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        var submissions = submissionRepository.findByUserIdOrderBySubmittedAtDesc(user.getId());

        model.addAttribute("user", user);
        model.addAttribute("submissions", submissions);
        model.addAttribute("title", "Submission History");

        return "student/history";
    }
}
