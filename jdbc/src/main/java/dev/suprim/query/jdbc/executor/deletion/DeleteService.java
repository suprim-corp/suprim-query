package dev.suprim.query.jdbc.executor.deletion;

import dev.suprim.query.exception.DbException;

public interface DeleteService {
    int delete(
            String dbId,
            String schemaName,
            String tableName,
            String filter
    ) throws DbException;
}
