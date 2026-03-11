package com.opensensemap.edu.connector;

import lombok.Data;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WebDatabaseConnector - Extended version of NRW DatabaseConnector for web applications
 *
 * This class maintains compatibility with the original DatabaseConnector API
 * This is the core component that will execute student SQL queries safely.
 * while adding web-specific features like:
 * - Enhanced security validation
 * - Query execution metrics
 * - JSON-compatible result format
 * - Better error handling
 */
@Component
public class WebDatabaseConnector {

    private final DataSource dataSource;
    private QueryResult currentQueryResult = null;
    private String message = null;
    private Long executionTimeMs = null;

    public WebDatabaseConnector(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Execute a SQL statement (SELECT only for student safety)
     * Compatible with original NRW DatabaseConnector API
     *
     * @param sqlStatement SQL query to execute
     */
    public void executeStatement(String sqlStatement) {
        if (sqlStatement == null || sqlStatement.trim().isEmpty()) {
            message = "Error: Empty query";
            currentQueryResult = null;
            return;
        }

        // Security validation
        SecurityValidation validation = validateQuery(sqlStatement);
        if (!validation.isValid()) {
            message = "Error: " + validation.getErrorMessage();
            currentQueryResult = null;
            return;
        }

        long startTime = System.currentTimeMillis();

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // Set query timeout (10 seconds)
            stmt.setQueryTimeout(10);

            // Set read-only mode for safety
            conn.setReadOnly(true);

            // Execute query
            boolean hasResults = stmt.execute(sqlStatement);

            if (hasResults) {
                ResultSet rs = stmt.getResultSet();
                currentQueryResult = new QueryResult(rs);
                executionTimeMs = System.currentTimeMillis() - startTime;

                message = String.format("✓ Query executed successfully. %d row(s) returned in %dms",
                        currentQueryResult.getRowCount(), executionTimeMs);

                rs.close();
            } else {
                currentQueryResult = null;
                message = "Query executed but returned no results";
            }

        } catch (SQLException e) {
            currentQueryResult = null;
            executionTimeMs = System.currentTimeMillis() - startTime;
            message = "✗ SQL Error: " + e.getMessage();

            // Provide helpful hints for common errors
            if (e.getMessage().contains("does not exist")) {
                message += "\n💡 Hint: Check your table and column names";
            } else if (e.getMessage().contains("syntax error")) {
                message += "\n💡 Hint: Check your SQL syntax";
            } else if (e.getMessage().contains("timeout") || e.getMessage().contains("cancelled")) {
                message += "\n💡 Hint: Query took too long. Try using LIMIT or more specific WHERE clauses";
            }
        }
    }

    /**
     * Get the result of the last query
     * Compatible with original NRW DatabaseConnector API
     *
     * @return QueryResult or null if query failed
     */
    public QueryResult getCurrentQueryResult() {
        return currentQueryResult;
    }

    /**
     * Get error or status message from last operation
     * Compatible with original NRW DatabaseConnector API
     *
     * @return Message string
     */
    public String getErrorMessage() {
        return message;
    }

    /**
     * Get execution time of last query
     *
     * @return Execution time in milliseconds
     */
    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }

    /**
     * Remove SQL comments from a query
     * Handles both single-line (--) and multi-line comments*/
    private String removeSqlComments(String query) {
        if (query == null) return "";
        
        // Remove single-line comments (-- comment)
        String result = query.replaceAll("--[^\r\n]*", "");
        
        // Remove multi-line comments (/* comment */)
        result = result.replaceAll("/\\*[\\s\\S]*?\\*/", "");
        
        return result.trim();
    }

    /**
     * Validate SQL query for security
     * Only SELECT statements are allowed in educational mode
     */
    private SecurityValidation validateQuery(String query) {
        // Remove SQL comments before validation
        String cleanedQuery = removeSqlComments(query);
        String upperQuery = cleanedQuery.toUpperCase().trim();

        // Check if query is empty after removing comments
        if (upperQuery.isEmpty()) {
            return new SecurityValidation(false, "Query is empty or contains only comments");
        }

        // Must start with SELECT
        if (!upperQuery.startsWith("SELECT")) {
            return new SecurityValidation(false,
                    "Only SELECT queries are allowed in educational mode. Found: " +
                            upperQuery.split("\\s+")[0]);
        }

        // Blacklist dangerous keywords (check in cleaned query)
        String[] forbiddenKeywords = {
                "DROP", "DELETE", "INSERT", "UPDATE", "TRUNCATE",
                "ALTER", "CREATE", "GRANT", "REVOKE", "EXECUTE",
                "CALL", "EXEC", "COPY", "VACUUM"
        };

        for (String keyword : forbiddenKeywords) {
            if (upperQuery.contains(keyword)) {
                return new SecurityValidation(false,
                        "Keyword '" + keyword + "' is not allowed in educational mode");
            }
        }

        // Prevent multiple statements (SQL injection protection)
        // Count semicolons in cleaned query
        long semicolonCount = cleanedQuery.chars().filter(ch -> ch == ';').count();
        if (semicolonCount > 1 || (semicolonCount == 1 && !cleanedQuery.trim().endsWith(";"))) {
            return new SecurityValidation(false,
                    "Multiple queries are not allowed. Use only one SELECT statement");
        }

        return new SecurityValidation(true, null);
    }

    /**
     * Inner class representing query results
     * Compatible with original NRW DatabaseConnector API
     */
    @Data
    public static class QueryResult {
        private String[][] data;
        private String[] columnNames;
        private String[] columnTypes;
        private int rowCount;
        private int columnCount;

        /**
         * Constructor - converts JDBC ResultSet to 2D array
         */
        public QueryResult(ResultSet rs) throws SQLException {
            // Get metadata
            ResultSetMetaData metaData = rs.getMetaData();
            columnCount = metaData.getColumnCount();

            // Extract column information
            columnNames = new String[columnCount];
            columnTypes = new String[columnCount];

            for (int i = 0; i < columnCount; i++) {
                columnNames[i] = metaData.getColumnName(i + 1);
                columnTypes[i] = metaData.getColumnTypeName(i + 1);
            }

            // Extract data
            List<String[]> rows = new ArrayList<>();
            while (rs.next()) {
                String[] row = new String[columnCount];
                for (int i = 0; i < columnCount; i++) {
                    Object value = rs.getObject(i + 1);
                    row[i] = (value != null) ? value.toString() : null;
                }
                rows.add(row);
            }

            // Convert to 2D array
            rowCount = rows.size();
            data = rows.toArray(new String[0][]);
        }

        /**
         * Convert result to JSON-compatible format for web display
         */
        public List<Map<String, Object>> toMapList() {
            List<Map<String, Object>> result = new ArrayList<>();

            for (String[] row : data) {
                Map<String, Object> rowMap = new HashMap<>();
                for (int i = 0; i < columnNames.length; i++) {
                    rowMap.put(columnNames[i], row[i]);
                }
                result.add(rowMap);
            }

            return result;
        }

        /**
         * Print results to console (for debugging/testing)
         */
        public void print() {
            System.out.println("\n" + "=".repeat(80));

            // Print column headers
            for (String colName : columnNames) {
                System.out.printf("%-20s", colName);
            }
            System.out.println("\n" + "-".repeat(80));

            // Print data rows
            for (String[] row : data) {
                for (String cell : row) {
                    String displayValue = (cell != null) ? cell : "NULL";
                    System.out.printf("%-20s", displayValue);
                }
                System.out.println();
            }

            System.out.println("=".repeat(80));
            System.out.printf("Rows: %d | Columns: %d\n", rowCount, columnCount);
        }
    }

    /**
     * Inner class for security validation results
     */
    @Data
    private static class SecurityValidation {
        private final boolean valid;
        private final String errorMessage;

        public SecurityValidation(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
    }
}