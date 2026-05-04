package dev.suprim.query.rsql.handler;

import dev.suprim.query.dialect.Dialect;
import dev.suprim.query.exception.DbException;
import dev.suprim.query.model.DbColumn;
import dev.suprim.query.model.DbWhere;

import java.util.List;
import java.util.Map;

public interface OperatorHandler {
    String PREFIX = ":";

    /**
     * Renders the SQL expression fragment for this RSQL operator.
     * <p>
     * Implementations must call {@link #reviewAndSetParam} to register parameter
     * values; do not call {@code paramMap.put()} directly.
     *
     * @param dialect  active database dialect for type conversion
     * @param column   resolved DB column
     * @param dbWhere  WHERE context (table info, paramMap)
     * @param value    RSQL operand as string
     * @param type     Java type mapped from JDBC metadata
     * @param paramMap named parameter map to populate for JDBC binding
     * @return SQL fragment, e.g. {@code t."name" = :t_name}
     */
    String handle(
            Dialect dialect,
            DbColumn column,
            DbWhere dbWhere,
            String value,
            Class<?> type,
            Map<String, Object> paramMap
    ) throws DbException;

    /**
     * Registers a named SQL parameter into paramMap, deduplicating keys by appending
     * a numeric suffix when a key already exists. The suffix is deterministic (count-based),
     * not random, so the same RSQL expression always produces the same param keys.
     *
     * @return the actual key used (may differ from {@code key} if a duplicate was found)
     */
    default String reviewAndSetParam(
            String key,
            Object value,
            Map<String, Object> paramMap
    ) {
        if (paramMap.containsKey(key)) {
            // Count existing entries that share the same base key to produce a stable suffix.
            // e.g. key="name" already exists → suffix = 1 → newKey = "name_1"
            int suffix = (int) paramMap.keySet().stream()
                    .filter(k -> k.equals(key) || k.startsWith(key + "_"))
                    .count();
            String newKey = key + "_" + suffix;
            paramMap.put(newKey, value);
            return newKey;
        }
        paramMap.put(key, value);
        return key;
    }

    default String handle(
            Dialect dialect,
            DbColumn column,
            DbWhere dbWhere,
            List<String> value,
            Class<?> type,
            Map<String, Object> paramMap
    ) throws DbException {
        return handle(dialect, column, dbWhere, value.getFirst(), type, paramMap);
    }
}
