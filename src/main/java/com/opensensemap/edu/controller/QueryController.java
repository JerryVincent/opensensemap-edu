package com.opensensemap.edu.controller;

import com.opensensemap.edu.service.QueryExecutionService;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * QueryController - REST API for executing SQL queries
 *
 * Endpoints:
 * POST /api/query/execute - Execute a SQL query
 * POST /api/query/test - Execute a query without tracking (for playground)
 */
@RestController
@RequestMapping("/api/query")
@CrossOrigin(origins = "*")
public class QueryController {

    private final QueryExecutionService queryExecutionService;

    public QueryController(QueryExecutionService queryExecutionService) {
        this.queryExecutionService = queryExecutionService;
    }

    /**
     * Execute a SQL query
     *
     * @param request Query request containing SQL and user/task info
     * @return Query execution result with data and feedback
     */
    @PostMapping("/execute")
    public ResponseEntity<?> executeQuery(@RequestBody QueryRequest request) {

        // Validate request
        if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    new ErrorResponse("Query text is required")
            );
        }

        try {
            // Execute query - userId can be null for anonymous users
            // Progress will only be saved if userId is not null
            QueryExecutionService.QueryExecutionResult result =
                    queryExecutionService.executeQuery(
                            request.getUserId(), // Can be null
                            request.getTaskId(),
                            request.getQuery()
                    );

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    new ErrorResponse("Query execution failed: " + e.getMessage())
            );
        }
    }

    /**
     * Test endpoint - execute query without task validation (for SQL playground)
     */
    @PostMapping("/test")
    public ResponseEntity<?> testQuery(@RequestBody QueryRequest request) {

        if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    new ErrorResponse("Query text is required")
            );
        }

        try {
            // Execute without user or task tracking
            QueryExecutionService.QueryExecutionResult result =
                    queryExecutionService.executeQuery(null, null, request.getQuery());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    new ErrorResponse("Query execution failed: " + e.getMessage())
            );
        }
    }

    /**
     * Request DTO
     */
    @Data
    public static class QueryRequest {
        private String query;
        private Long userId;  // Can be null for anonymous users
        private Long taskId;
    }

    /**
     * Error response DTO
     */
    @Data
    public static class ErrorResponse {
        private final String error;
        private final long timestamp;

        public ErrorResponse(String error) {
            this.error = error;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
