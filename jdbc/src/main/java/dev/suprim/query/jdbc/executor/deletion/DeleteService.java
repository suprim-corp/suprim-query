package dev.suprim.query.jdbc.executor.deletion;

import dev.suprim.query.exception.DbException;

import java.util.List;

public interface DeleteService {
    int delete(
            String dbId,
            String schemaName,
            String tableName,
            String filter
    ) throws DbException;

    /**
     * Execute multiple delete operations in a single transaction.
     * Each filter scopes a separate delete statement.
     * Rolls back all changes if any single operation fails.
     *
     * @return total number of rows deleted across all operations
     */
    int deleteBulk(
            String dbId,
            String schemaName,
            String tableName,
            List<String> filters
    ) throws DbException;
}
