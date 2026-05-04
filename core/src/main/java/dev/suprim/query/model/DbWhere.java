package dev.suprim.query.model;

import java.util.List;
import java.util.Map;

public record DbWhere(
        String tableName,
        DbTable table,
        List<DbColumn> columns,
        Map<String, Object> paramMap,
        String op,
        List<DbTable> allTables
) {
    public boolean isDelete() {
        return op.equalsIgnoreCase("delete");
    }

    /**
     * Registers a named SQL parameter accumulated during RSQL → SQL conversion.
     * <p>
     * Operator handlers should call this method instead of calling
     * {@code paramMap().put()} directly, so that mutation is explicit and
     * discoverable at the call site.
     *
     * @param key   the named parameter key (without the ":" prefix)
     * @param value the parameter value to bind
     */
    public void addParam(String key, Object value) {
        paramMap.put(key, value);
    }
}
