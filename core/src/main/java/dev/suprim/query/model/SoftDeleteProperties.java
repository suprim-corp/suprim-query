package dev.suprim.query.model;

import java.util.List;

import static java.util.Objects.isNull;

/**
 * Configuration for soft-delete behavior.
 *
 * @param enabled whether soft-delete is active
 * @param column  the timestamp column used for soft-delete (default: deleted_at)
 * @param tables  optional allowlist of tables; if empty, applies to all tables
 */
public record SoftDeleteProperties(
        boolean enabled,
        String column,
        List<String> tables
) {
    public SoftDeleteProperties {
        if (isNull(column) || column.isBlank()) {
            column = "deleted_at";
        }
        tables = isNull(tables) ? List.of() : List.copyOf(tables);
    }

    /**
     * Returns true if soft-delete applies to the given table.
     * When the tables list is empty, soft-delete applies to all tables.
     */
    public boolean appliesTo(String tableName) {
        if (!enabled) return false;
        if (tables.isEmpty()) return true;
        return tables.contains(tableName);
    }

    public static SoftDeleteProperties disabled() {
        return new SoftDeleteProperties(false, "deleted_at", List.of());
    }
}
