package dev.suprim.query.model;

import java.util.List;

import static java.util.Objects.isNull;

/**
 * Configuration for upsert (INSERT ... ON CONFLICT) operations.
 *
 * @param conflictColumns columns forming the conflict target (ON CONFLICT (col1, col2))
 * @param updateColumns   columns to update on conflict; if null/empty, DO NOTHING is used
 */
public record UpsertConfig(
        List<String> conflictColumns,
        List<String> updateColumns
) {
    public UpsertConfig {
        if (isNull(conflictColumns) || conflictColumns.isEmpty()) {
            throw new IllegalArgumentException("conflictColumns must not be null or empty");
        }
    }

    public boolean isDoNothing() {
        return isNull(updateColumns) || updateColumns.isEmpty();
    }
}
