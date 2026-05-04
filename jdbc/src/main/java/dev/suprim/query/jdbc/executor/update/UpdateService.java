package dev.suprim.query.jdbc.executor.update;

import dev.suprim.query.exception.DbException;

import java.util.Map;

public interface UpdateService {
    int patch(
            String dbId,
            String schemaName,
            String tableName,
            Map<String, Object> data,
            String filter
    ) throws DbException;
}
