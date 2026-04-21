package com.opensensemap.edu.service;

import com.opensensemap.edu.connector.WebDatabaseConnector;
import com.opensensemap.edu.model.entity.QuerySubmission;
import com.opensensemap.edu.model.entity.Task;
import com.opensensemap.edu.model.entity.EduUser;
import com.opensensemap.edu.model.entity.UserProgress;
import com.opensensemap.edu.repository.QuerySubmissionRepository;
import com.opensensemap.edu.repository.TaskRepository;
import com.opensensemap.edu.repository.EduUserRepository;
import com.opensensemap.edu.repository.UserProgressRepository;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * QueryExecutionService - Handles SQL query execution and validation
 *
 * This service:
 * - Executes student SQL queries safely
 * - Validates results against expected output (using sample_solution or validation_query)
 * - Provides intelligent feedback
 * - Logs submissions for learning analytics
 * - Updates user progress when tasks are completed correctly
 * - Blocks access to sensitive tables and strips sensitive columns from results
 */
@Service
public class QueryExecutionService {

    private final WebDatabaseConnector connector;
    private final QuerySubmissionRepository submissionRepository;
    private final TaskRepository taskRepository;
    private final EduUserRepository userRepository;
    private final UserProgressRepository progressRepository;

    // ================================================================
    // SECURITY: Tables and columns that must not be exposed to students
    // ================================================================

    /** Tables that students are NOT allowed to query */
    private static final Set<String> BLOCKED_TABLES = Set.of(
            "user",             // openSenseMap user accounts (quoted as "user" in SQL)
            "profile",          // openSenseMap user profiles
            "edu_user",         // application user accounts (passwords, emails)
            "user_progress",    // internal progress tracking
            "query_submission"  // internal submission logs
    );

    /** Columns that must be stripped from query results */
    private static final Set<String> SENSITIVE_COLUMNS = Set.of(
            "user_id",
            "userid",
            "apikey",
            "api_key",
            "email",
            "password",
            "password_hash",
            "passwordhash",
            "token",
            "secret",
            "owner_name"
    );

    /**
     * Pattern to extract table names from SQL queries.
     * Matches: FROM tablename, JOIN tablename, FROM "tablename"
     */
    private static final Pattern TABLE_PATTERN = Pattern.compile(
            "(?:FROM|JOIN)\\s+\"?([a-zA-Z_][a-zA-Z0-9_]*)\"?",
            Pattern.CASE_INSENSITIVE
    );

    public QueryExecutionService(
            WebDatabaseConnector connector,
            QuerySubmissionRepository submissionRepository,
            TaskRepository taskRepository,
            EduUserRepository userRepository,
            UserProgressRepository progressRepository) {
        this.connector = connector;
        this.submissionRepository = submissionRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.progressRepository = progressRepository;
    }

    /**
     * Execute a student query with validation
     *
     * @param userId User ID (can be null for anonymous users)
     * @param taskId Task ID (optional, for validation)
     * @param queryText SQL query text
     * @return Execution result with feedback
     */
    @Transactional
    public QueryExecutionResult executeQuery(Long userId, Long taskId, String queryText) {

        // ============================================================
        // SECURITY CHECK: Block queries on sensitive tables
        // ============================================================
        String blockedTable = findBlockedTable(queryText);
        if (blockedTable != null) {
            QueryExecutionResult blocked = new QueryExecutionResult();
            blocked.setSuccess(false);
            blocked.setQueryText(queryText);
            blocked.setRowsReturned(0);
            blocked.setExecutionTimeMs(0L);
            blocked.setIsCorrect(false);
            blocked.setFeedback("✗ Access denied: The table '" + blockedTable +
                    "' contains protected data and cannot be queried. " +
                    "Use tables like device, sensor, measurement, or the provided views (view_devices, view_sensors).");
            blocked.setErrorMessage("Access to table '" + blockedTable + "' is not permitted.");
            return blocked;
        }

        // Execute student's query
        connector.executeStatement(queryText);

        // Get basic execution info
        boolean wasSuccessful = connector.getCurrentQueryResult() != null;
        int rowsReturned = wasSuccessful ? connector.getCurrentQueryResult().getRowCount() : 0;
        Long executionTimeMs = connector.getExecutionTimeMs();
        String errorMessage = !wasSuccessful ? connector.getErrorMessage() : null;

        // Store student's result for comparison
        WebDatabaseConnector.QueryResult studentResult = wasSuccessful ? connector.getCurrentQueryResult() : null;

        // Validate if this is a task submission
        boolean isCorrect = false;
        String feedback = errorMessage != null ? errorMessage : "Query executed successfully.";
        Task task = null;

        if (wasSuccessful && taskId != null) {
            task = taskRepository.findById(taskId).orElse(null);
            if (task != null) {
                ValidationResult validation = validateAgainstTask(task, studentResult);
                isCorrect = validation.isCorrect();
                feedback = validation.getFeedback();
            }
        }

        // Build result first
        QueryExecutionResult result = new QueryExecutionResult();
        result.setSuccess(wasSuccessful);
        result.setQueryText(queryText);
        result.setRowsReturned(rowsReturned);
        result.setExecutionTimeMs(executionTimeMs);
        result.setIsCorrect(isCorrect);
        result.setFeedback(feedback);
        result.setErrorMessage(errorMessage);

        // Only save submission and update progress if user is logged in
        if (userId != null) {
            EduUser user = userRepository.findById(userId).orElse(null);

            if (user != null) {
                // Save submission to database
                QuerySubmission submission = saveSubmission(
                        user, task, queryText,
                        wasSuccessful, rowsReturned, executionTimeMs,
                        errorMessage, isCorrect, feedback
                );
                result.setSubmissionId(submission.getId());

                // Update user progress if correct
                if (isCorrect && task != null) {
                    updateUserProgress(user, task);
                }
            }
        }

        // Add result data if successful (use student's original result)
        if (wasSuccessful && studentResult != null) {
            // ============================================================
            // SECURITY: Strip sensitive columns before sending to client
            // ============================================================
            SanitizedResult sanitized = stripSensitiveColumns(
                    studentResult.getColumnNames(), studentResult.getData());

            // ============================================================
            // SAFETY: Cap displayed rows at 500 to prevent browser freeze
            // (validation already ran on the full uncapped result above)
            // ============================================================
            final int MAX_DISPLAY_ROWS = 500;
            String[][] displayData = sanitized.data;
            boolean wasCapped = false;
            if (displayData.length > MAX_DISPLAY_ROWS) {
                displayData = Arrays.copyOf(displayData, MAX_DISPLAY_ROWS);
                wasCapped = true;
            }

            result.setColumns(sanitized.columns);
            result.setData(displayData);
            result.setRowsReturned(sanitized.data.length); // report actual count, not capped
            result.setResultMap(buildSanitizedMapList(sanitized.columns, displayData));

            // Append cap notice to feedback if rows were truncated
            if (wasCapped) {
                result.setFeedback(result.getFeedback() +
                        " (Showing first " + MAX_DISPLAY_ROWS + " of " + sanitized.data.length + " rows)");
            }
        }

        return result;
    }

    // ================================================================
    // SECURITY METHODS
    // ================================================================

    /**
     * Check if the query references any blocked table.
     * Returns the blocked table name if found, null if safe.
     */
    private String findBlockedTable(String query) {
        if (query == null) return null;

        // Remove SQL comments (-- and /* */)
        String cleaned = query.replaceAll("--[^\n]*", " ")
                .replaceAll("/\\*.*?\\*/", " ");

        Matcher matcher = TABLE_PATTERN.matcher(cleaned);
        while (matcher.find()) {
            String tableName = matcher.group(1).toLowerCase().trim();
            if (BLOCKED_TABLES.contains(tableName)) {
                return tableName;
            }
        }
        return null;
    }

    /**
     * Remove sensitive columns from query results.
     * Returns new arrays with sensitive columns filtered out.
     */
    private SanitizedResult stripSensitiveColumns(String[] columns, String[][] data) {
        if (columns == null || columns.length == 0) {
            return new SanitizedResult(columns, data);
        }

        // Find indices of safe (non-sensitive) columns
        List<Integer> safeIndices = new ArrayList<>();
        List<String> safeColumns = new ArrayList<>();

        for (int i = 0; i < columns.length; i++) {
            String colLower = columns[i].toLowerCase().trim();
            if (!SENSITIVE_COLUMNS.contains(colLower)) {
                safeIndices.add(i);
                safeColumns.add(columns[i]);
            }
        }

        // If nothing was stripped, return original
        if (safeIndices.size() == columns.length) {
            return new SanitizedResult(columns, data);
        }

        // Build filtered column array
        String[] filteredColumns = safeColumns.toArray(new String[0]);

        // Build filtered data array
        String[][] filteredData = new String[data.length][safeIndices.size()];
        for (int row = 0; row < data.length; row++) {
            for (int col = 0; col < safeIndices.size(); col++) {
                filteredData[row][col] = data[row][safeIndices.get(col)];
            }
        }

        return new SanitizedResult(filteredColumns, filteredData);
    }

    /**
     * Build a List<Map> from sanitized columns and data (replaces the original resultMap)
     */
    private List<Map<String, Object>> buildSanitizedMapList(String[] columns, String[][] data) {
        List<Map<String, Object>> mapList = new ArrayList<>();
        for (String[] row : data) {
            Map<String, Object> rowMap = new LinkedHashMap<>();
            for (int i = 0; i < columns.length && i < row.length; i++) {
                rowMap.put(columns[i], row[i]);
            }
            mapList.add(rowMap);
        }
        return mapList;
    }

    /** Simple holder for sanitized result data */
    private static class SanitizedResult {
        final String[] columns;
        final String[][] data;

        SanitizedResult(String[] columns, String[][] data) {
            this.columns = columns;
            this.data = data;
        }
    }

    // ================================================================
    // VALIDATION METHODS (unchanged)
    // ================================================================

    /**
     * Validate query result against expected output for a task
     * Uses validation_query if available, otherwise uses sample_solution
     */
    private ValidationResult validateAgainstTask(Task task, WebDatabaseConnector.QueryResult studentResult) {
        if (task == null) {
            return new ValidationResult(false, "Task not found");
        }

        // Debug: Log what we have
        System.out.println("=== VALIDATION DEBUG ===");
        System.out.println("Task ID: " + task.getId());
        System.out.println("Task Title: " + task.getTitle());
        System.out.println("Validation Query: " + task.getValidationQuery());
        System.out.println("Sample Solution: " + task.getSampleSolution());
        System.out.println("========================");

        // Determine which query to use for validation
        String validationSql = null;

        if (task.getValidationQuery() != null && !task.getValidationQuery().trim().isEmpty()) {
            // Use explicit validation query
            validationSql = task.getValidationQuery().trim();
            System.out.println("Using validation_query for validation");
        } else if (task.getSampleSolution() != null && !task.getSampleSolution().trim().isEmpty()) {
            // Use sample solution as validation query
            validationSql = task.getSampleSolution().trim();
            System.out.println("Using sample_solution for validation: " + validationSql);
        }

        // If no validation query available, we cannot validate
        if (validationSql == null) {
            return new ValidationResult(false,
                    "⚠ Query executed but cannot validate - no sample solution defined for this task. " +
                            "Please ask your instructor to check your result.");
        }

        // Execute validation query to get expected results
        try {
            connector.executeStatement(validationSql);
            WebDatabaseConnector.QueryResult expectedResult = connector.getCurrentQueryResult();

            if (expectedResult == null) {
                return new ValidationResult(false,
                        "⚠ Could not validate your answer. The validation query failed to execute.");
            }

            // Compare results
            ComparisonResult comparison = compareResults(studentResult, expectedResult);

            if (comparison.isMatch()) {
                return new ValidationResult(true, String.format(
                        "✓ Excellent! Your query returned the correct result. You earned %d points!",
                        task.getPoints()
                ));
            } else {
                // Provide helpful feedback based on what's different
                return new ValidationResult(false, comparison.getFeedback());
            }

        } catch (Exception e) {
            return new ValidationResult(false,
                    "⚠ Error validating your query: " + e.getMessage());
        }
    }

    /**
     * Compare two query results and provide detailed feedback
     */
    private ComparisonResult compareResults(
            WebDatabaseConnector.QueryResult studentResult,
            WebDatabaseConnector.QueryResult expectedResult) {

        // Check if student returned no results
        if (studentResult.getRowCount() == 0 && expectedResult.getRowCount() > 0) {
            return new ComparisonResult(false,
                    "✗ Your query returned no results, but it should return " + expectedResult.getRowCount() + " row(s). " +
                            "Check your WHERE clause and table names.");
        }

        // Check row count
        if (studentResult.getRowCount() != expectedResult.getRowCount()) {
            return new ComparisonResult(false, String.format(
                    "✗ Row count mismatch: Your query returned %d row(s), but expected %d row(s). " +
                            "Review your filtering conditions (WHERE clause) or check if you're querying the right data.",
                    studentResult.getRowCount(), expectedResult.getRowCount()
            ));
        }

        // Check column count
        if (studentResult.getColumnCount() != expectedResult.getColumnCount()) {
            return new ComparisonResult(false, String.format(
                    "✗ Column count mismatch: Your query returned %d column(s), but expected %d column(s). " +
                            "Make sure you're selecting the correct columns as specified in the task.",
                    studentResult.getColumnCount(), expectedResult.getColumnCount()
            ));
        }

        // NOTE: Column names/aliases are intentionally NOT compared.
        // Students may write "SELECT device.name" (returns "name") while the
        // sample solution uses "SELECT d.name AS device_name" (returns "device_name").
        // Both are valid as long as the underlying data is correct.

        // Compare actual data content
        String studentData = normalizeResultData(studentResult);
        String expectedData = normalizeResultData(expectedResult);

        if (!studentData.equals(expectedData)) {
            return new ComparisonResult(false,
                    "✗ Data mismatch: Your query returned different data than expected. " +
                            "The row count and columns are correct, but the actual values differ. " +
                            "Check your filtering conditions and make sure you're querying the right records.");
        }

        // All checks passed
        return new ComparisonResult(true, "Match!");
    }

    /**
     * Normalize result data for comparison
     * Converts to sorted string representation to handle row ordering differences
     */
    private String normalizeResultData(WebDatabaseConnector.QueryResult result) {
        String[][] data = result.getData();

        // Convert each row to a string and sort
        List<String> rows = Arrays.stream(data)
                .map(row -> Arrays.stream(row)
                        .map(cell -> cell == null ? "NULL" : cell.trim().toLowerCase())
                        .collect(Collectors.joining("|")))
                .sorted()
                .collect(Collectors.toList());

        return String.join("\n", rows);
    }

    /**
     * Save query submission to database
     */
    private QuerySubmission saveSubmission(
            EduUser user, Task task, String queryText,
            boolean wasSuccessful, int rowsReturned, Long executionTimeMs,
            String errorMessage, boolean isCorrect, String feedback) {

        // Calculate attempt number
        int attemptNumber = 1;
        if (task != null && user != null) {
            List<QuerySubmission> previousAttempts =
                    submissionRepository.findByUserIdAndTaskIdOrderByAttemptNumberDesc(user.getId(), task.getId());
            if (!previousAttempts.isEmpty()) {
                attemptNumber = previousAttempts.get(0).getAttemptNumber() + 1;
            }
        }

        // Create submission with proper entity relationships
        QuerySubmission submission = new QuerySubmission();
        submission.setUser(user);
        submission.setTask(task);
        submission.setQueryText(queryText);
        submission.setWasSuccessful(wasSuccessful);
        submission.setRowsReturned(rowsReturned);
        submission.setExecutionTimeMs(executionTimeMs != null ? executionTimeMs.intValue() : null);
        submission.setErrorMessage(errorMessage);
        submission.setIsCorrect(isCorrect);
        submission.setFeedback(feedback);
        submission.setAttemptNumber(attemptNumber);

        return submissionRepository.save(submission);
    }

    /**
     * Update user progress when a task is completed correctly
     */
    private void updateUserProgress(EduUser user, Task task) {
        // Get or create progress for this scenario
        Long scenarioId = task.getScenario().getId();

        UserProgress progress = progressRepository.findByUserIdAndScenarioId(user.getId(), scenarioId)
                .orElseGet(() -> {
                    UserProgress newProgress = new UserProgress();
                    newProgress.setUser(user);
                    newProgress.setScenario(task.getScenario());
                    newProgress.setTotalTasks(task.getScenario().getTotalTasks());
                    newProgress.setTasksCompleted(0);
                    newProgress.setScore(0);
                    newProgress.setStatus(UserProgress.ProgressStatus.IN_PROGRESS);
                    return newProgress;
                });

        // Check if this task was already completed by checking previous correct submissions
        List<QuerySubmission> previousCorrect = submissionRepository
                .findByUserIdAndTaskIdOrderByAttemptNumberDesc(user.getId(), task.getId());

        // Only count the task as newly completed if this is the first correct submission
        boolean alreadyCompleted = previousCorrect.stream()
                .skip(1) // Skip the current submission we just saved
                .anyMatch(QuerySubmission::isCorrectResult);

        if (!alreadyCompleted) {
            progress.completeTask(task.getPoints());
        }

        progressRepository.save(progress);
    }

    /**
     * Result class for query execution
     */
    @Data
    public static class QueryExecutionResult {
        private boolean success;
        private String queryText;
        private Integer rowsReturned;
        private Long executionTimeMs;
        private Boolean isCorrect;
        private String feedback;
        private String errorMessage;
        private Long submissionId;

        // Result data (if successful)
        private String[] columns;
        private String[][] data;
        private List<Map<String, Object>> resultMap;
    }

    /**
     * Result class for validation
     */
    @Data
    private static class ValidationResult {
        private final boolean correct;
        private final String feedback;

        public ValidationResult(boolean correct, String feedback) {
            this.correct = correct;
            this.feedback = feedback;
        }
    }

    /**
     * Result class for detailed comparison
     */
    @Data
    private static class ComparisonResult {
        private final boolean match;
        private final String feedback;

        public ComparisonResult(boolean match, String feedback) {
            this.match = match;
            this.feedback = feedback;
        }
    }
}