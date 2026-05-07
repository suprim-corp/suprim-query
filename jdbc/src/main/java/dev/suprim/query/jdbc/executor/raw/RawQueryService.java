package dev.suprim.query.jdbc.executor.raw;

import dev.suprim.query.exception.DbException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Escape hatch for executing arbitrary SQL while still benefiting from
 * connection routing and transaction management provided by {@code JdbcManager}.
 *
 * <p><strong>Warning:</strong> Callers are responsible for preventing SQL injection.
 * Always use named parameters ({@code :paramName}) instead of string concatenation.
 */
public interface RawQueryService {

    /**
     * Execute a SELECT query that returns a single row.
     *
     * @param dbId   database identifier for connection routing
     * @param sql    SQL with named parameters (e.g. {@code SELECT * FROM users WHERE id = :id})
     * @param params named parameter values (must not be null; use empty map for no params)
     * @return the row as a column-value map, or {@link Optional#empty()} if no row found
     * @throws DbException if the database is not found or query fails
     */
    Optional<Map<String, Object>> queryOne(String dbId, String sql, Map<String, Object> params) throws DbException;

    /**
     * Execute a SELECT query that returns multiple rows.
     *
     * @param dbId   database identifier for connection routing
     * @param sql    SQL with named parameters
     * @param params named parameter values (must not be null; use empty map for no params)
     * @return list of rows, each as a column-value map; empty list if no results
     * @throws DbException if the database is not found or query fails
     */
    List<Map<String, Object>> queryList(String dbId, String sql, Map<String, Object> params) throws DbException;

    /**
     * Execute a write statement (INSERT, UPDATE, DELETE) within a transaction.
     *
     * @param dbId   database identifier for connection routing
     * @param sql    SQL with named parameters
     * @param params named parameter values (must not be null; use empty map for no params)
     * @return number of rows affected
     * @throws DbException if the database is not found or execution fails
     */
    int execute(String dbId, String sql, Map<String, Object> params) throws DbException;
}
