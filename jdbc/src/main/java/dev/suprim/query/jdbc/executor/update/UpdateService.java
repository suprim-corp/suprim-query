package dev.suprim.query.jdbc.executor.update;

import dev.suprim.query.exception.DbException;
import dev.suprim.query.model.dto.BulkUpdate;

import java.util.List;
import java.util.Map;

public interface UpdateService {
    int patch(
            String dbId,
            String schemaName,
            String tableName,
            Map<String, Object> data,
            String filter
    ) throws DbException;

    /**
     * Execute multiple update operations in a single transaction.
     * Each {@link BulkUpdate} entry specifies its own data and filter.
     * Rolls back all changes if any single operation fails.
     *
     * @return total number of rows affected across all operations
     */
    int patchBulk(
            String dbId,
            String schemaName,
            String tableName,
            List<BulkUpdate> updates
    ) throws DbException;
}
